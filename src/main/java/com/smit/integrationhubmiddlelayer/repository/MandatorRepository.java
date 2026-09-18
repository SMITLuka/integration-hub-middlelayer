package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.Mandator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MandatorRepository extends JpaRepository<Mandator, Long>
{
    Page<Mandator> findByNameContainingIgnoreCase(String search, Pageable pageable);

    Optional<Mandator> findByExternalMandatorId(String externalMandatorId);

    @org.springframework.data.jpa.repository.Query(
            "select count(distinct c.mandator.id) from Company c") //$NON-NLS-1$
    long countMandatorsWithAtLeastOneCompany();
}
