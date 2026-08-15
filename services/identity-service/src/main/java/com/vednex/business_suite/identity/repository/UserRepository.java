package com.vednex.business_suite.identity.repository;

import java.util.Optional;
import java.util.UUID;

import com.vednex.business_suite.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	@Query("select u from User u where lower(u.email) = lower(:email)")
	Optional<User> findByEmailIgnoreCase(@Param("email") String email);

	@Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
	boolean existsByEmailIgnoreCase(@Param("email") String email);

	java.util.List<User> findByIdIn(java.util.Collection<UUID> ids);

}
