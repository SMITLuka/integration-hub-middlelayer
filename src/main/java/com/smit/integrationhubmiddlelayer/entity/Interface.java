package com.smit.integrationhubmiddlelayer.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * An integration definition (e.g. "Volvo Grip Api"). Optionally carries a Mapping Template
 * and/or a Configuration Template that Companies can instantiate.
 */
@Entity
@Table(name = "interface_definition") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Interface
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true) //$NON-NLS-1$
    private String name;

    @Column(name = "dms_to_middleware_url") //$NON-NLS-1$
    private String dmsToMiddlewareUrl;

    @Column(name = "oem_to_middleware_url") //$NON-NLS-1$
    private String oemToMiddlewareUrl;

    @Column(name = "middleware_to_oem_url") //$NON-NLS-1$
    private String middlewareToOemUrl;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "interface_additional_data", joinColumns = @JoinColumn(name = "interface_id")) //$NON-NLS-1$ //$NON-NLS-2$
    @MapKeyColumn(name = "data_key") //$NON-NLS-1$
    @Column(name = "data_value") //$NON-NLS-1$
    private Map<String, String> additionalData = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false) //$NON-NLS-1$
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at") //$NON-NLS-1$
    private Instant updatedAt;
}
