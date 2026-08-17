package com.miniproject.verificationApp.repository;

import com.miniproject.verificationApp.model.GovernmentIdVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GovernmentIdVerificationRepository
        extends JpaRepository<GovernmentIdVerification, Long> {

    List<GovernmentIdVerification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<GovernmentIdVerification> findByUserIdAndStatus(
            Long userId,
            GovernmentIdVerification.Status status
    );

    Optional<GovernmentIdVerification>
    findTopByUserIdAndPlaceNameAndStatusOrderByCreatedAtDesc(
            Long userId,
            String placeName,
            GovernmentIdVerification.Status status
    );
}