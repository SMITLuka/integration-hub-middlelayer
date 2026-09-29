package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.CompanyMappingValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyMappingValueRepository extends JpaRepository<CompanyMappingValue, Long>
{
    List<CompanyMappingValue> findByCompanyMappingId(Long companyMappingId);

    Optional<CompanyMappingValue> findByCompanyMappingIdAndTemplateRowId(Long companyMappingId, Long templateRowId);

    boolean existsByTemplateRowId(Long templateRowId);
}
