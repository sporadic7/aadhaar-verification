package com.company.aadhaar.repository;

import com.company.aadhaar.entity.AadhaarVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AadhaarVerificationRepository extends JpaRepository<AadhaarVerification, Long> {
}


