package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateEntryDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.entity.ConfigValueType;
import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplate;
import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplateEntry;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationRepository;
import com.smit.integrationhubmiddlelayer.repository.ConfigurationTemplateRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for saving an existing Configuration Template: entries must be merged in
 * place (stable ids, no delete-and-reinsert), otherwise re-saving the same keys violated the
 * UNIQUE (template, key) constraint and returned a 500.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationTemplateServiceTest
{
    private static final Long INTERFACE_ID = 1L;

    @Mock
    private InterfaceRepository interfaceRepository;

    @Mock
    private ConfigurationTemplateRepository configurationTemplateRepository;

    @Mock
    private CompanyConfigurationRepository companyConfigurationRepository;

    @InjectMocks
    private ConfigurationTemplateService configurationTemplateService;

    private ConfigurationTemplate template;
    private ConfigurationTemplateEntry spaUrl;

    @BeforeEach
    void setUp()
    {
        Interface interfaceEntity = Interface.builder().id(INTERFACE_ID).name("SPA Integration").build(); //$NON-NLS-1$
        template = ConfigurationTemplate.builder().id(5L).interfaceEntity(interfaceEntity).entries(new ArrayList<>()).build();
        spaUrl = entry(1L, "SPA_URL", ""); //$NON-NLS-1$ //$NON-NLS-2$
        template.getEntries().add(spaUrl);
        template.getEntries().add(entry(2L, "SPA_USER", "")); //$NON-NLS-1$ //$NON-NLS-2$

        when(interfaceRepository.findById(INTERFACE_ID)).thenReturn(Optional.of(interfaceEntity));
        when(configurationTemplateRepository.findByInterfaceEntityId(INTERFACE_ID)).thenReturn(Optional.of(template));
        when(configurationTemplateRepository.save(any(ConfigurationTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void replace_updatesExistingEntriesInPlace_removesMissing_addsNew()
    {
        ConfigurationTemplateUpsertRequest request = new ConfigurationTemplateUpsertRequest();
        request.setEntries(List.of(
                dto(1L, "SPA_URL", "https://spa.cometengine.com/api/pool/spaadm/comet2"), //$NON-NLS-1$ //$NON-NLS-2$
                dto(null, "SPA_SERVICE_ID", "b0"))); //$NON-NLS-1$ //$NON-NLS-2$

        configurationTemplateService.replace(INTERFACE_ID, request);

        assertThat(template.getEntries()).hasSize(2);
        assertThat(template.getEntries().get(0)).isSameAs(spaUrl);
        assertThat(spaUrl.getId()).isEqualTo(1L);
        assertThat(spaUrl.getDefaultValue()).isEqualTo("https://spa.cometengine.com/api/pool/spaadm/comet2"); //$NON-NLS-1$
        assertThat(template.getEntries().get(1).getKey()).isEqualTo("SPA_SERVICE_ID"); //$NON-NLS-1$
        assertThat(template.getEntries().get(1).getConfigurationTemplate()).isSameAs(template);
        assertThat(template.getEntries()).extracting(ConfigurationTemplateEntry::getKey).doesNotContain("SPA_USER"); //$NON-NLS-1$
    }

    @Test
    void replace_flushesRemovalsBeforeInsertingEntryThatReusesRemovedKey()
    {
        ConfigurationTemplateUpsertRequest request = new ConfigurationTemplateUpsertRequest();
        request.setEntries(List.of(dto(1L, "SPA_URL", ""), dto(null, "SPA_USER", "smit"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        configurationTemplateService.replace(INTERFACE_ID, request);

        InOrder order = inOrder(configurationTemplateRepository);
        order.verify(configurationTemplateRepository, times(2)).flush();
        order.verify(configurationTemplateRepository).save(template);
        assertThat(template.getEntries()).extracting(ConfigurationTemplateEntry::getKey).containsExactly("SPA_URL", "SPA_USER"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_doesNotFlush_whenTemplateIsNew()
    {
        when(configurationTemplateRepository.findByInterfaceEntityId(INTERFACE_ID)).thenReturn(Optional.empty());
        ConfigurationTemplateUpsertRequest request = new ConfigurationTemplateUpsertRequest();
        request.setEntries(List.of(dto(null, "SPA_URL", ""))); //$NON-NLS-1$ //$NON-NLS-2$

        configurationTemplateService.create(INTERFACE_ID, request);

        verify(configurationTemplateRepository, never()).flush();
    }

    private ConfigurationTemplateEntry entry(Long id, String key, String defaultValue)
    {
        return ConfigurationTemplateEntry.builder().id(id).configurationTemplate(template).key(key)
                .type(ConfigValueType.TEXT).defaultValue(defaultValue).sortOrder(id.intValue()).build();
    }

    private static ConfigurationTemplateEntryDto dto(Long id, String key, String defaultValue)
    {
        return new ConfigurationTemplateEntryDto(id, key, ConfigValueType.TEXT, defaultValue, "", "", 0); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
