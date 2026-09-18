package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long>
{
    Optional<Company> findByMandatorIdAndDmsCompanyId(Long mandatorId, String dmsCompanyId);
}
