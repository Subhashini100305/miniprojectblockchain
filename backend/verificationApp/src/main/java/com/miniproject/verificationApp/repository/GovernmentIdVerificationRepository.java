package com.miniproject.verificationApp.repository;

import com.miniproject.verificationApp.model.GovernmentIdVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GovernmentIdVerificationRepository extends JpaRepository<GovernmentIdVerification, Long> {
    List<GovernmentIdVerification> findByUserId(Long userId);
}
