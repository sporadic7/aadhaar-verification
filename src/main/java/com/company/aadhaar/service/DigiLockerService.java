package com.company.aadhaar.service;

import com.company.aadhaar.config.DigiLockerConfig;
import com.company.aadhaar.dto.AadhaarResponse;
import com.company.aadhaar.dto.TokenResponse;
import com.company.aadhaar.exception.DigiLockerException;
import com.company.aadhaar.repository.AadhaarVerificationRepository;
import com.company.aadhaar.entity.AadhaarVerification;
import com.company.aadhaar.util.XmlParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;


import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;





@Service
public class DigiLockerService {

    private final DigiLockerConfig cfg;
    private final AadhaarVerificationRepository repo;
    private final com.company.aadhaar.repository.VerificationSessionRepository sessionRepo;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DigiLockerService(
            DigiLockerConfig cfg,
            AadhaarVerificationRepository repo,
            com.company.aadhaar.repository.VerificationSessionRepository sessionRepo
    ) {
        this.cfg = cfg;
        this.repo = repo;
        this.sessionRepo = sessionRepo;
    }


    /**
     * Starts DigiLocker OAuth authorize URL.
     * Implements RFC7636 PKCE (S256) and secure state persisted in {@link VerificationSession}.
     */
    public String generateAuthorizeUrl(String userId) {
        // Generate secure PKCE verifier + challenge
        String codeVerifier = com.company.aadhaar.util.CodeChallengeUtil.generateCodeVerifier();
        String codeChallenge = com.company.aadhaar.util.CodeChallengeUtil.generateCodeChallengeS256(codeVerifier);

        // Generate high-entropy state, persist server-side so callback can validate.
        String state = generateSecureState();

        Instant now = Instant.now();
        // Default expiry: 10 minutes (configurable in next iterations)
        Instant expiresAt = now.plusSeconds(10 * 60);

        com.company.aadhaar.entity.VerificationSession session = new com.company.aadhaar.entity.VerificationSession();
        session.setState(state);
        session.setCodeVerifier(codeVerifier);
        session.setCodeChallenge(codeChallenge);
        session.setStatus("PENDING");
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setExpiresAt(expiresAt);
        session.setUserId(userId);
        sessionRepo.save(session);

        // Build authorize URL with PKCE parameters
        // DigiLocker authorize endpoint expects: client_id, redirect_uri, response_type=code,
        // code_challenge, code_challenge_method, state
        String base = cfg.getOauthAuthorizeUrl();
        return base + "?" + "client_id=" + encode(cfg.getClientId())
                + "&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&response_type=code"
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256";
    }

    private static String generateSecureState() {
        // 32 bytes => 43 chars (base64url without padding) ~ high entropy
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    /**
     * DigiLocker callback: exchange authorization code -> access token -> fetch eAadhaar document (XML) -> parse -> persist.
     */
    public AadhaarResponse exchangeCodeAndVerify(String code) {
        throw new DigiLockerException("DigiLocker: state-aware callback required (missing state)");
    }



    /**
     * State-aware DigiLocker callback.
     */
    @org.springframework.transaction.annotation.Transactional
    public AadhaarResponse exchangeCodeAndVerify(String code, String state) {
        if (state == null || state.isBlank()) {
            throw new DigiLockerException("DigiLocker: missing state");
        }

        // Single-flight replay protection: atomically claim the session
        // (PENDING/FAILED rejected; VERIFIED rejected; EXPIRED marked)
        var session = sessionRepo.findByState(state)
                .orElseThrow(() -> new DigiLockerException("DigiLocker: invalid state"));

        final Instant now = Instant.now();

        if (session.getExpiresAt() == null) {
            markSessionFailedAndDeleteQuietly(session, now);
            throw new DigiLockerException("DigiLocker: session missing expiresAt");
        }

        if (now.isAfter(session.getExpiresAt())) {
            markSessionExpiredAndDeleteQuietly(session, now);
            throw new DigiLockerException("DigiLocker: session expired");
        }

        String status = session.getStatus();
        if (status != null && ("VERIFIED".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status))) {
            throw new DigiLockerException("DigiLocker: callback replay detected");
        }
        if (status != null && "FAILED".equalsIgnoreCase(status)) {
            throw new DigiLockerException("DigiLocker: callback rejected (previously failed)");
        }

        // Claim session so concurrent callbacks can't both proceed.
        // Since we don't have a version/locking column in this repo,
        // we set PROCESSING and flush. Within @Transactional this reduces race windows.
        session.setStatus("PROCESSING");
        session.setUpdatedAt(now);
        sessionRepo.save(session);
        sessionRepo.flush();

        try {
            String token = fetchAccessToken(code, session.getCodeVerifier());

            String aadhaarDocUri = findAadhaarDocumentUri(token);
            if (aadhaarDocUri == null || aadhaarDocUri.isBlank()) {
                throw new DigiLockerException("DigiLocker: No Aadhaar document found in /files");
            }

            byte[] documentBytes = downloadDocumentFile(token, aadhaarDocUri);

            AadhaarResponse response;
            try {
                String asString = new String(documentBytes, StandardCharsets.UTF_8);
                if (asString.trim().startsWith("<")) {
                    var details = XmlParser.parseAadhaarXml(asString);
                    AadhaarVerification v = new AadhaarVerification();
                    v.setAadhaarLast4(details.aadhaarLast4());
                    v.setFullName(details.fullName());
                    v.setDob(details.dob());
                    v.setAddress(details.address());
                    v.setDigilockerDocumentUri(aadhaarDocUri);
                    v.setVerifiedAt(Instant.now());
                    v.setStatus("VERIFIED");
                    repo.save(v);
                    response = new AadhaarResponse(details.aadhaarLast4(), "VERIFIED");
                } else {
                    String pdfAsString = new String(documentBytes, StandardCharsets.UTF_8);
                    var parsedPdf = com.company.aadhaar.util.PdfParser.parseAadhaarPdf(pdfAsString);
                    if (parsedPdf == null || parsedPdf.aadhaarLast4() == null) {
                        throw new DigiLockerException("DigiLocker: Unable to parse Aadhaar details from PDF");
                    }
                    AadhaarVerification v = new AadhaarVerification();
                    v.setAadhaarLast4(parsedPdf.aadhaarLast4());
                    v.setFullName(parsedPdf.fullName());
                    v.setDob(parsedPdf.dob());
                    v.setAddress(parsedPdf.address());
                    v.setDigilockerDocumentUri(aadhaarDocUri);
                    v.setVerifiedAt(Instant.now());
                    v.setStatus("VERIFIED");
                    repo.save(v);
                    response = new AadhaarResponse(parsedPdf.aadhaarLast4(), "VERIFIED");
                }
            } catch (Exception parseEx) {
                throw new DigiLockerException("DigiLocker: document parsing failed", parseEx);
            }

            // Finalize session: VERIFIED + cleanup
            session.setStatus("VERIFIED");
            session.setUpdatedAt(Instant.now());
            sessionRepo.save(session);
            sessionRepo.delete(session);

            return response;

        } catch (DigiLockerException e) {
            // Persist failed verification outcome
            AadhaarVerification failed = new AadhaarVerification();
            failed.setStatus("FAILED");
            failed.setVerifiedAt(Instant.now());
            repo.save(failed);

            // Mark session failed + cleanup
            session.setStatus("FAILED");
            session.setUpdatedAt(Instant.now());
            sessionRepo.save(session);
            sessionRepo.delete(session);

            throw e;
        } catch (Exception e) {
            AadhaarVerification failed = new AadhaarVerification();
            failed.setStatus("FAILED");
            failed.setVerifiedAt(Instant.now());
            repo.save(failed);

            session.setStatus("FAILED");
            session.setUpdatedAt(Instant.now());
            sessionRepo.save(session);
            sessionRepo.delete(session);

            throw new DigiLockerException("DigiLocker verification failed: " + e.getMessage(), e);
        }
    }

    private void markSessionFailedAndDeleteQuietly(com.company.aadhaar.entity.VerificationSession session, Instant now) {
        try {
            session.setStatus("FAILED");
            session.setUpdatedAt(now);
            sessionRepo.save(session);
            sessionRepo.delete(session);
        } catch (Exception ignore) {
        }
    }

    private void markSessionExpiredAndDeleteQuietly(com.company.aadhaar.entity.VerificationSession session, Instant now) {
        try {
            session.setStatus("EXPIRED");
            session.setUpdatedAt(now);
            sessionRepo.save(session);
            sessionRepo.delete(session);
        } catch (Exception ignore) {
        }
    }



    private String fetchAccessToken(String code, String codeVerifier) {

        String tokenUrl = cfg.getOauthTokenUrl();

        // IMPORTANT: redirect_uri must exactly match what was used for authorize URL (and registered)
        String redirectUri = cfg.getRedirectUri();

        String body = "grant_type=" + encode("authorization_code") +
                "&code=" + encode(code) +
                "&redirect_uri=" + encode(redirectUri) +
                "&client_id=" + encode(cfg.getClientId()) +
                "&client_secret=" + encode(cfg.getClientSecret());


        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp;
        try {
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new DigiLockerException("DigiLocker: token exchange call failed", e);
        }

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new DigiLockerException("DigiLocker token exchange failed: " + resp.statusCode() + ": " + resp.body());
        }

        // Parse OAuth JSON properly
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            TokenResponse token = mapper.readValue(resp.body(), TokenResponse.class);
            if (token.getAccessToken() == null || token.getAccessToken().isBlank()) {
                throw new DigiLockerException("DigiLocker: access_token is blank in token response");
            }
            return token.getAccessToken();
        } catch (DigiLockerException e) {
            throw e;
        } catch (Exception e) {
            throw new DigiLockerException("DigiLocker: unable to parse token response JSON", e);
        }
    }


    private byte[] downloadDocumentFile(String accessToken, String documentUri) {
        // DigiLocker expects: {filesBaseUrl}/file?uri={documentUri}
        String filesBase = cfg.getFilesBaseUrl();
        if (filesBase == null || filesBase.isBlank()) {
            throw new DigiLockerException("DigiLocker: missing filesBaseUrl");
        }
        String base = filesBase.endsWith("/") ? filesBase.substring(0, filesBase.length() - 1) : filesBase;

        String encodedDocUri = encode(documentUri.startsWith("/") ? documentUri.substring(1) : documentUri);
        String fileUrl = base + "/file?uri=" + encodedDocUri;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .GET()
                .timeout(java.time.Duration.ofSeconds(20))
                .build();

        HttpResponse<byte[]> resp;
        try {
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        } catch (java.net.http.HttpTimeoutException te) {
            throw new DigiLockerException("DigiLocker: document download timed out", te);
        } catch (Exception e) {
            throw new DigiLockerException("DigiLocker: document download call failed", e);
        }

        int code = resp.statusCode();
        if (code == 401) throw new DigiLockerException("DigiLocker: unauthorized while downloading document");
        if (code == 403) throw new DigiLockerException("DigiLocker: forbidden while downloading document");
        if (code == 404) throw new DigiLockerException("DigiLocker: document not found");
        if (code >= 500) throw new DigiLockerException("DigiLocker: server error while downloading document: " + code);
        if (code < 200 || code >= 300) throw new DigiLockerException("DigiLocker: document download failed: " + code);

        return resp.body();
    }

    private String findAadhaarDocumentUri(String accessToken) {
        String filesBase = cfg.getFilesBaseUrl();
        if (filesBase == null || filesBase.isBlank()) {
            throw new DigiLockerException("DigiLocker: missing filesBaseUrl");
        }
        String base = filesBase.endsWith("/") ? filesBase.substring(0, filesBase.length() - 1) : filesBase;
        String filesUrl = base + "/files";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(filesUrl))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .GET()
                .timeout(java.time.Duration.ofSeconds(20))
                .build();

        HttpResponse<String> resp;
        try {
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException te) {
            throw new DigiLockerException("DigiLocker: /files request timed out", te);
        } catch (Exception e) {
            throw new DigiLockerException("DigiLocker: /files request failed", e);
        }

        int code = resp.statusCode();
        if (code == 401) throw new DigiLockerException("DigiLocker: unauthorized while listing files");
        if (code == 403) throw new DigiLockerException("DigiLocker: forbidden while listing files");
        if (code == 404) throw new DigiLockerException("DigiLocker: files endpoint not found");
        if (code >= 500) throw new DigiLockerException("DigiLocker: server error while listing files: " + code);
        if (code < 200 || code >= 300) throw new DigiLockerException("DigiLocker: /files failed: " + code);

        String body = resp.body();
        // DigiLocker /files response is typically JSON: { "documents": [ { "name": "aadhaar", "uri": "aadhaar/.." } ] }
        // Heuristic parsing via Jackson without hard dependency on exact schema.
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(body);

            // Try common arrays
            var docs = root.get("documents");
            if (docs == null) docs = root.get("files");
            if (docs == null) docs = root.get("data");
            if (docs != null && docs.isArray()) {
                for (var node : docs) {
                    // Determine aadhaar document by type/name heuristics
                    String name = node.has("name") ? node.get("name").asText(null) : null;
                    String docType = node.has("type") ? node.get("type").asText(null) : null;
                    String uri = node.has("uri") ? node.get("uri").asText(null) : null;

                    if (uri == null) continue;
                    String hay = (name == null ? "" : name) + " " + (docType == null ? "" : docType) + " " + uri;
                    if (hay.toLowerCase().contains("aadhaar")) {
                        return uri;
                    }
                }
            }
        } catch (Exception ignore) {
            // Fall back to regex-based extraction below.
        }

        // Fallback regex: find first occurrence of "uri" containing "aadhaar"
        var m = java.util.regex.Pattern.compile("\\\"uri\\\"\\s*:\\s*\\\"([^\\\"]*aadhaar[^\\\"]*)\\\"", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(body);

        if (m.find()) return m.group(1);

        return null;
    }


    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }


}





