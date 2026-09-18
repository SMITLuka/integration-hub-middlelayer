package com.smit.integrationhubmiddlelayer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
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
 * The schema of configuration keys an Interface expects (Key/Type/Default Value/Expression/Description).
 */
@Entity
@Table(name = "configuration_template") //$NON-NLS-1$
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConfigurationTemplate
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "interface_id", nullable = false, unique = true) //$NON-NLS-1$
    private Interface interfaceEntity;

    @Builder.Default
    @OneToMany(mappedBy = "configurationTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc") //$NON-NLS-1$
    private List<ConfigurationTemplateEntry> entries = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false) //$NON-NLS-1$
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at") //$NON-NLS-1$
    private Instant updatedAt;
}
