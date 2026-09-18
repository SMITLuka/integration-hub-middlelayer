package com.smit.integrationhubmiddlelayer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import java.util.List;

/**
 * An instance of an Interface's Mapping Template applied to one Company.
 */
@Entity
@Table(name = "company_mapping") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompanyMapping
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false) //$NON-NLS-1$
    private Company company;

    @ManyToOne(optional = false)
    @JoinColumn(name = "interface_id", nullable = false) //$NON-NLS-1$
    private Interface interfaceEntity;

    @Builder.Default
    @OneToMany(mappedBy = "companyMapping", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyMappingValue> values = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false) //$NON-NLS-1$
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at") //$NON-NLS-1$
    private Instant updatedAt;
}
