package com.company.aadhaar.dto;

public class DocumentResponse {
    private String documentType;
    private String documentUrl;

    public DocumentResponse() {
    }

    public DocumentResponse(String documentType, String documentUrl) {
        this.documentType = documentType;
        this.documentUrl = documentUrl;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }
}

