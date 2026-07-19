package com.company.aadhaar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DigiLockerConfig {

    @Value("${digilocker.client-id}")
    private String clientId;

    @Value("${digilocker.client-secret}")
    private String clientSecret;

    @Value("${digilocker.redirect-uri}")
    private String redirectUri;

    @Value("${digilocker.oauth-authorize-url}")
    private String oauthAuthorizeUrl;

    @Value("${digilocker.oauth-token-url}")
    private String oauthTokenUrl;

    @Value("${digilocker.files-base-url}")
    private String filesBaseUrl;

    public String getClientId() {

        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getOauthAuthorizeUrl() {
        return oauthAuthorizeUrl;
    }

    public String getOauthTokenUrl() {
        return oauthTokenUrl;
    }

    public String getFilesBaseUrl() {
        return filesBaseUrl;
    }
}




