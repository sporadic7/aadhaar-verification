package com.company.aadhaar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "aadhaar_verification")
public class AadhaarVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /**
     * Never store full Aadhaar number.
     * Store only last 4 digits extracted from eAadhaar.
     */
    @Column(length = 4)
    private String aadhaarLast4;

    private String fullName;
    private String dob;
    private String address;

    /** DigiLocker document URI (so we can audit which document was used) */
    @Column(length = 1024)
    private String digilockerDocumentUri;

    private Instant verifiedAt;

    /** e.g. VERIFIED / FAILED */
    @Column(length = 32)
    private String status;

    public AadhaarVerification() {
    }

    public Long getId() {
        return id;
    }

    public String getAadhaarLast4() {
        return aadhaarLast4;
    }

    public void setAadhaarLast4(String aadhaarLast4) {
        this.aadhaarLast4 = aadhaarLast4;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDigilockerDocumentUri() {
        return digilockerDocumentUri;
    }

    public void setDigilockerDocumentUri(String digilockerDocumentUri) {
        this.digilockerDocumentUri = digilockerDocumentUri;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}


