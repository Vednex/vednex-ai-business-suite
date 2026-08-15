package com.vednex.business_suite.company.repository;

import java.util.UUID;

import com.vednex.business_suite.company.domain.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, UUID> {
}
