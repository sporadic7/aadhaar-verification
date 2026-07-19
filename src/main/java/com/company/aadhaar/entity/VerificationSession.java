package com.company.aadhaar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "verification_session")
public class VerificationSession {

    @Id
    @Column(length = 64)
    private String state;

    @Column(length = 128)
    private String codeVerifier;

    @Column(length = 64)
    private String codeChallenge;

@Column(length = 32)
    private String status; // PENDING / VERIFIED / FAILED

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * ISO timestamp after which callback should be rejected.
     */
    private Instant expiresAt;

    /**
     * Optional ERP user identifier that initiated the OAuth flow.
     */
    @Column(length = 128)
    private String userId;


    public VerificationSession() {}

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

