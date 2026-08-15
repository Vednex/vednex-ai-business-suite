package com.vednex.business_suite.identity.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.vednex.business_suite.identity.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

	Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

	Optional<EmailVerificationToken> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);

	@Modifying
	@Query("""
			update EmailVerificationToken token
			set token.usedAt = :usedAt
			where token.userId = :userId
			  and token.usedAt is null
			  and token.expiresAt > :now
			""")
	int markActiveUnusedTokensUsed(
			@Param("userId") UUID userId,
			@Param("usedAt") Instant usedAt,
			@Param("now") Instant now
	);

}
