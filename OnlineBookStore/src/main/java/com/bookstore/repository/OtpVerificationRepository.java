package com.bookstore.repository;

import com.bookstore.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/*
 * Repository for OTP DB operations.
 * Two custom methods needed beyond standard JpaRepository.
 */
@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

	/*
	 * Finds the most recently generated OTP for a given email. Used in
	 * OtpService.verifyOtp() to check if OTP is valid. OrderByCreatedAtDesc →
	 * always gets latest OTP if multiple exist.
	 */
	Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);

	/*
	 * Deletes all OTP records for a given email. Called in generateAndSendOtp()
	 * before creating a new OTP. Ensures one active OTP per email at any time.
	 */
	void deleteByEmail(String email);
}