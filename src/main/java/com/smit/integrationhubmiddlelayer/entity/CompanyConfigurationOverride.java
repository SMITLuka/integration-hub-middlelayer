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
 * The level-1 (highest priority) override for one Configuration Template entry, on one
 * Company's Configuration instance of an Interface. Presence of a row here means OVERRIDE;
 * absence falls through to Company, then Mandator, then Template Additional Data / defaults.
 */
@Entity
@Table(name = "company_configuration_override") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompanyConfigurationOverride
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_configuration_id", nullable = false) //$NON-NLS-1$
    private CompanyConfiguration companyConfiguration;

    @ManyToOne(optional = false)
    @JoinColumn(name = "template_entry_id", nullable = false) //$NON-NLS-1$
    private ConfigurationTemplateEntry templateEntry;

    @Column(name = "override_value") //$NON-NLS-1$
    private String value;
}
