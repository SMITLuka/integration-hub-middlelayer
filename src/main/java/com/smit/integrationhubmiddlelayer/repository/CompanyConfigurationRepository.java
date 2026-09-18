package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.CompanyConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyConfigurationRepository extends JpaRepository<CompanyConfiguration, Long>
{
    List<CompanyConfiguration> findByCompanyId(Long companyId);

    Optional<CompanyConfiguration> findByCompanyIdAndInterfaceEntityId(Long companyId, Long interfaceId);

    List<CompanyConfiguration> findByInterfaceEntityId(Long interfaceId);

    boolean existsByInterfaceEntityId(Long interfaceId);
}
