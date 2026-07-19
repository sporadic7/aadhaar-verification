package com.company.aadhaar.controller;

import com.company.aadhaar.dto.AadhaarResponse;
import com.company.aadhaar.exception.DigiLockerException;
import com.company.aadhaar.repository.AadhaarVerificationRepository;
import com.company.aadhaar.entity.AadhaarVerification;
import com.company.aadhaar.util.XmlParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final AadhaarVerificationRepository repo;

    public DemoController(AadhaarVerificationRepository repo) {
        this.repo = repo;
    }

    /**
     * Mock verification to showcase the whole flow on localhost without DigiLocker credentials.
     * Uses existing XmlParser + persists into H2.
     */
    @PostMapping("/digilocker/mock-verify")
    public ResponseEntity<AadhaarResponse> mockVerify() {
        // Sample XML (eAadhaar-like). XmlParser looks for tags: Name, Dob, AadhaarNumber, and address components.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document>" +
                "  <PrintLetter>" +
                "    <Name>VIKAS KUMAR</Name>" +
                "    <Dob>1990-01-15</Dob>" +
                "    <House>12</House>" +
                "    <Street>MG Road</Street>" +
                "    <Locality>Near City Center</Locality>" +
                "    <District>Delhi</District>" +
                "    <State>Delhi</State>" +
                "    <Pincode>110001</Pincode>" +
                "    <AadhaarNumber>1234-5678-9012</AadhaarNumber>" +
                "  </PrintLetter>" +
                "</Document>";

        XmlParser.ParsedAadhaarDetails details = XmlParser.parseAadhaarXml(xml);
        if (details == null || details.aadhaarLast4() == null) {
            throw new DigiLockerException("Mock verify: unable to parse sample eAadhaar XML");
        }

        AadhaarVerification v = new AadhaarVerification();
        v.setAadhaarLast4(details.aadhaarLast4());
        v.setFullName(details.fullName());
        v.setDob(details.dob());
        v.setAddress(details.address());
        v.setVerifiedAt(Instant.now());
        v.setStatus("VERIFIED");
        v.setDigilockerDocumentUri("MOCK");

        repo.save(v);

        return ResponseEntity.ok(new AadhaarResponse(details.aadhaarLast4(), "VERIFIED"));
    }

    @GetMapping("/demo/aadhaar/verifications/recent")
    public ResponseEntity<?> recent() {

        var all = repo.findAll();
        all.sort((a, b) -> {
            Instant ia = a.getVerifiedAt();
            Instant ib = b.getVerifiedAt();
            if (ia == null && ib == null) return 0;
            if (ia == null) return 1;
            if (ib == null) return -1;
            return ib.compareTo(ia);
        });

        int n = Math.min(5, all.size());
        return ResponseEntity.ok(all.subList(0, n));
    }
}

