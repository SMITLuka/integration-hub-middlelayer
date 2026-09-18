package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigurationTemplateRepository extends JpaRepository<ConfigurationTemplate, Long>
{
    Optional<ConfigurationTemplate> findByInterfaceEntityId(Long interfaceId);
}
