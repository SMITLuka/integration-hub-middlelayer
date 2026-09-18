package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.CompanyMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyMappingRepository extends JpaRepository<CompanyMapping, Long>
{
    List<CompanyMapping> findByCompanyId(Long companyId);

    Optional<CompanyMapping> findByCompanyIdAndInterfaceEntityId(Long companyId, Long interfaceId);

    List<CompanyMapping> findByInterfaceEntityId(Long interfaceId);

    boolean existsByInterfaceEntityId(Long interfaceId);
}
