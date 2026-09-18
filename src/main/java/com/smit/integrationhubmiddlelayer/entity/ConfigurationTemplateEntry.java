package com.smit.integrationhubmiddlelayer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * One configuration key definition within a Configuration Template.
 */
@Entity
@Table(name = "configuration_template_entry") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConfigurationTemplateEntry
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "configuration_template_id", nullable = false) //$NON-NLS-1$
    private ConfigurationTemplate configurationTemplate;

    @Column(name = "config_key", nullable = false) //$NON-NLS-1$
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigValueType type;

    @Column(name = "default_value") //$NON-NLS-1$
    private String defaultValue;

    private String expression;

    private String description;

    @Column(name = "sort_order") //$NON-NLS-1$
    private Integer sortOrder;
}
