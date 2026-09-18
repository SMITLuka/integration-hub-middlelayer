package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.MappingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MappingTemplateRepository extends JpaRepository<MappingTemplate, Long>
{
    Optional<MappingTemplate> findByInterfaceEntityId(Long interfaceId);
}
