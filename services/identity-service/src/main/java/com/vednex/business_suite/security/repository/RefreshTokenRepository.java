package com.vednex.business_suite.security.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vednex.business_suite.security.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findByFamilyId(UUID familyId);

	List<RefreshToken> findByUserIdAndActiveTrue(UUID userId);

	@Modifying
	@Query("""
			update RefreshToken rt
			set rt.active = false, rt.revokedAt = :revokedAt
			where rt.familyId = :familyId and rt.active = true
			""")
	int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

	@Modifying
	@Query("""
			update RefreshToken rt
			set rt.active = false, rt.revokedAt = :revokedAt
			where rt.userId = :userId and rt.active = true
			""")
	int revokeActiveUserTokens(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

}
