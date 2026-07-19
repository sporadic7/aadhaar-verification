package com.company.aadhaar.service;

import com.company.aadhaar.dto.AadhaarResponse;
import org.springframework.stereotype.Service;

/**
 * ERP-facing service orchestrator.
 *
 * DigiLocker OAuth details are handled in {@link DigiLockerService}.
 */
@Service
public class AadhaarService {

    private final DigiLockerService digiLockerService;

    public AadhaarService(DigiLockerService digiLockerService) {
        this.digiLockerService = digiLockerService;
    }

    /**
     * Verify using DigiLocker callback query params.
     *
     * @param code OAuth authorization code
     * @param state OAuth state (used to lookup persisted VerificationSession)
     * @return Aadhaar verification response
     */
    public AadhaarResponse verifyWithDigiLocker(String code, String state) {
        return digiLockerService.exchangeCodeAndVerify(code, state);
    }
}



