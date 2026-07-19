package com.company.aadhaar.dto;

/**
 * ERP-facing compliant response.
 * We never return full Aadhaar number; only last 4 digits.
 */
public class AadhaarResponse {
    private String aadhaarLast4;
    private String status;

    public AadhaarResponse() {
    }

    public AadhaarResponse(String aadhaarLast4, String status) {
        this.aadhaarLast4 = aadhaarLast4;
        this.status = status;
    }

    public String getAadhaarLast4() {
        return aadhaarLast4;
    }

    public void setAadhaarLast4(String aadhaarLast4) {
        this.aadhaarLast4 = aadhaarLast4;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}


