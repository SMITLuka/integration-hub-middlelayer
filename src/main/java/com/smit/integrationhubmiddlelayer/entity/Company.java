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
import jakarta.persistence.ManyToOne;
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
 * A location/branch under a Mandator. Additional Data here is a sparse override map:
 * a key present here shadows the same key on the parent Mandator; a key absent here
 * is inherited from the Mandator.
 */
@Entity
@Table(name = "company") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Company
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mandator_id", nullable = false) //$NON-NLS-1$
    private Mandator mandator;

    @Column(nullable = false)
    private String name;

    @Column(name = "dms_company_id") //$NON-NLS-1$
    private String dmsCompanyId;

    private String location;
    private String address;

    @Column(name = "country_code") //$NON-NLS-1$
    private String countryCode;

    @Column(name = "customer_number") //$NON-NLS-1$
    private String customerNumber;

    @Column(name = "default_locale") //$NON-NLS-1$
    private String defaultLocale;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "company_additional_data", joinColumns = @JoinColumn(name = "company_id")) //$NON-NLS-1$ //$NON-NLS-2$
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
