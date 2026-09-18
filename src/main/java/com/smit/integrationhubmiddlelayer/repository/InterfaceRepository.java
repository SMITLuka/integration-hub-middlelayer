package com.smit.integrationhubmiddlelayer.repository;

import com.smit.integrationhubmiddlelayer.entity.Interface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InterfaceRepository extends JpaRepository<Interface, Long>
{
    Page<Interface> findByNameContainingIgnoreCase(String search, Pageable pageable);

    Optional<Interface> findByName(String name);

    @Query("select count(distinct i.id) from Interface i "
            + "where exists (select 1 from MappingTemplate mt where mt.interfaceEntity = i) "
            + "or exists (select 1 from ConfigurationTemplate ct where ct.interfaceEntity = i)") //$NON-NLS-1$
    long countInterfacesWithAnyTemplate();
}
