package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.CompanyConfigurationOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyConfigurationOverrideRepository extends JpaRepository<CompanyConfigurationOverride, Long>
{
    List<CompanyConfigurationOverride> findByCompanyConfigurationId(Long companyConfigurationId);

    Optional<CompanyConfigurationOverride> findByCompanyConfigurationIdAndTemplateEntryId(Long companyConfigurationId, Long templateEntryId);

    boolean existsByTemplateEntryId(Long templateEntryId);
}
