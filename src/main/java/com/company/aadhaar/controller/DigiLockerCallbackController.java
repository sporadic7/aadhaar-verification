package com.company.aadhaar.controller;

import com.company.aadhaar.dto.AadhaarResponse;
import com.company.aadhaar.service.DigiLockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/digilocker")
public class DigiLockerCallbackController {

    private final DigiLockerService digiLockerService;

    public DigiLockerCallbackController(DigiLockerService digiLockerService) {
        this.digiLockerService = digiLockerService;
    }

    /**
     * DigiLocker redirects back to our backend with authorization code.
     * Example: GET /api/digilocker/callback?code=abc123
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        AadhaarResponse response = digiLockerService.exchangeCodeAndVerify(code, state);
        return ResponseEntity.ok(response);
    }


}


