package com.company.aadhaar.repository;

import com.company.aadhaar.entity.VerificationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationSessionRepository extends JpaRepository<VerificationSession, String> {
    java.util.Optional<VerificationSession> findByState(String state);
}



