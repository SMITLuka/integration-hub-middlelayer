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
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A top-level tenant (e.g. a customer's legal entity) that owns one or more Companies.
 * Additional Data is an open-ended key-value bag; values placed here are inherited by
 * every Company under this Mandator unless a Company overrides the same key.
 */
@Entity
@Table(name = "mandator") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Mandator
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    private String system;
    private String customer;

    @Column(name = "external_mandator_id", unique = true) //$NON-NLS-1$
    private String externalMandatorId;

    private String country;
    private String locale;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "mandator_additional_data", joinColumns = @JoinColumn(name = "mandator_id")) //$NON-NLS-1$ //$NON-NLS-2$
    @MapKeyColumn(name = "data_key") //$NON-NLS-1$
    @Column(name = "data_value") //$NON-NLS-1$
    private Map<String, String> additionalData = new HashMap<>();

    @Builder.Default
    @OneToMany(mappedBy = "mandator", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Company> companies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false) //$NON-NLS-1$
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at") //$NON-NLS-1$
    private Instant updatedAt;
}
