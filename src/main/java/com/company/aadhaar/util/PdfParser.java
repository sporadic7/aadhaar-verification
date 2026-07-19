package com.company.aadhaar.util;

import java.nio.charset.StandardCharsets;

public class PdfParser {

    public record ParsedAadhaarDetails(String fullName, String dob, String address, String aadhaarLast4) {
    }

    /**
     * Best-effort Aadhaar extraction from a PDF passed as a String.
     *
     * NOTE:
     * - This project’s current DigiLockerService downloads documents as String (not raw bytes).
     * - PDF layouts vary by version.
     *
     * Implementation uses reflection for PDFBox classes so the repo compiles even if IDE
     * temporarily can’t resolve PDFBox imports.
     */
    public static ParsedAadhaarDetails parseAadhaarPdf(String pdfContent) {
        if (pdfContent == null || pdfContent.isBlank()) return null;

        try {
            byte[] bytes;
            String t = pdfContent.trim();
            if (t.length() > 100 && t.matches("^[A-Za-z0-9+/\\r\\n=]+$")) {
                bytes = java.util.Base64.getDecoder().decode(t);
            } else {
                bytes = pdfContent.getBytes(StandardCharsets.UTF_8);
            }

            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Object doc = pdDocumentClass.getMethod("load", byte[].class).invoke(null, bytes);

            try {
                Class<?> stripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
                Object stripper = stripperClass.getDeclaredConstructor().newInstance();

                // Optional method (ignore if missing)
                try {
                    stripperClass.getMethod("setSortByPosition", boolean.class).invoke(stripper, true);
                } catch (NoSuchMethodException ignore) {
                }

                String text = (String) stripperClass.getMethod("getText", pdDocumentClass).invoke(stripper, doc);

                String fullName = firstMatch(text, "(?i)(?:Name|N\\s*ame)\\s*[:\\-]?\\s*([^\\n\\r]{3,80})");
                String dob = firstMatch(text, "(?i)(?:DOB|D\\s*O\\s*B)\\s*[:\\-]?\\s*(\\d{4}-\\d{2}-\\d{2}|\\d{2}-\\d{2}-\\d{4})");

                String aadhaarLast4 = null;
                String aadhaar = firstMatch(text, "(\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4})");
                if (aadhaar != null) {
                    String digits = aadhaar.replaceAll("\\D", "");
                    if (digits.length() >= 4) aadhaarLast4 = digits.substring(digits.length() - 4);
                }

                String address = extractAddressHeuristic(text);

                if (fullName == null && dob == null && address == null && aadhaarLast4 == null) return null;
                return new ParsedAadhaarDetails(clean(fullName), clean(dob), clean(address), clean(aadhaarLast4));
            } finally {
                try {
                    pdDocumentClass.getMethod("close").invoke(doc);
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstMatch(String text, String regex) {
        if (text == null) return null;
        var p = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.MULTILINE);
        var m = p.matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String extractAddressHeuristic(String text) {
        if (text == null) return null;

        String[] markersStart = new String[]{"Address", "ADDRES"};
        String[] markersEnd = new String[]{"Mobile", "E-mail", "Phone", "Gender", "Date"};

        for (String start : markersStart) {
            int sIdx = indexOfIgnoreCase(text, start);
            if (sIdx < 0) continue;

            int eIdx = text.length();
            for (String end : markersEnd) {
                int tmp = indexOfIgnoreCase(text, end);
                if (tmp > sIdx && tmp < eIdx) eIdx = tmp;
            }

            String chunk = text.substring(sIdx, eIdx);
            chunk = chunk.replaceAll("(?i)" + java.util.regex.Pattern.quote(start) + "\\s*[:\\-]?", "");
            chunk = chunk.replaceAll("\\s+", " ").trim();
            return chunk.isEmpty() ? null : chunk;
        }

        return null;
    }

    private static int indexOfIgnoreCase(String s, String sub) {
        return s.toLowerCase().indexOf(sub.toLowerCase());
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

