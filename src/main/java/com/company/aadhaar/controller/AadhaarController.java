package com.company.aadhaar.controller;

import com.company.aadhaar.config.DigiLockerConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController

@RequestMapping("/api/aadhaar")
public class AadhaarController {

    private final DigiLockerConfig cfg;
    private final com.company.aadhaar.service.DigiLockerService digiLockerService;


    @GetMapping("/health")

    public String health() {
        return "aadhaar-controller-ok";
    }

    private final com.company.aadhaar.repository.AadhaarVerificationRepository repo;

    public AadhaarController(DigiLockerConfig cfg,
                              com.company.aadhaar.service.DigiLockerService digiLockerService,
                              com.company.aadhaar.repository.AadhaarVerificationRepository repo) {
        this.cfg = cfg;
        this.digiLockerService = digiLockerService;
        this.repo = repo;
    }



    @GetMapping("/verifications/recent")
    public ResponseEntity<?> recent() {
        var all = repo.findAll();
        all.sort((a, b) -> {
            java.time.Instant ia = a.getVerifiedAt();
            java.time.Instant ib = b.getVerifiedAt();
            if (ia == null && ib == null) return 0;
            if (ia == null) return 1;
            if (ib == null) return -1;
            return ib.compareTo(ia);
        });

        int n = Math.min(5, all.size());
        return ResponseEntity.ok(all.subList(0, n));
    }



    /**
     * Starts DigiLocker OAuth. Your ERP should redirect the user to this URL.
     */
    @GetMapping("/digilocker/authorize-url")
    public ResponseEntity<String> authorizeUrl(@RequestParam(name = "userId", required = false) String userId) {
        String url = digiLockerService.generateAuthorizeUrl(userId);
        return ResponseEntity.ok(url);
    }


}


