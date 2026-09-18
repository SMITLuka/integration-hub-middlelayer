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
 * One field-mapping row (Descriptor / Third Party Value pair) within a Mapping Template Section.
 */
@Entity
@Table(name = "mapping_template_row") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MappingTemplateRow
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id", nullable = false) //$NON-NLS-1$
    private MappingTemplateSection section;

    @Column(nullable = false)
    private String descriptor;

    @Column(name = "third_party_value") //$NON-NLS-1$
    private String thirdPartyValue;

    @Column(name = "sort_order") //$NON-NLS-1$
    private Integer sortOrder;
}
