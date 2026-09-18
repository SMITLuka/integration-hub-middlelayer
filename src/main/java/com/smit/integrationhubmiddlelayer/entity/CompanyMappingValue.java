package com.smit.integrationhubmiddlelayer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An override of one Mapping Template row's Third Party Value, for one Company's Mapping.
 * Presence of a row here means OVERRIDE; absence means the template row's own value applies.
 */
@Entity
@Table(name = "company_mapping_value") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompanyMappingValue
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_mapping_id", nullable = false) //$NON-NLS-1$
    private CompanyMapping companyMapping;

    @ManyToOne(optional = false)
    @JoinColumn(name = "template_row_id", nullable = false) //$NON-NLS-1$
    private MappingTemplateRow templateRow;

    @Column(name = "override_value") //$NON-NLS-1$
    private String value;
}
