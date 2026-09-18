package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.DashboardSummaryDto;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import com.smit.integrationhubmiddlelayer.repository.MandatorRepository;
import com.smit.integrationhubmiddlelayer.repository.MappingTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the counts shown on the dashboard's summary cards.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService
{
    private final MandatorRepository mandatorRepository;
    private final InterfaceRepository interfaceRepository;
    private final MappingTemplateRepository mappingTemplateRepository;

    public DashboardService(MandatorRepository mandatorRepository, InterfaceRepository interfaceRepository,
            MappingTemplateRepository mappingTemplateRepository)
    {
        this.mandatorRepository = mandatorRepository;
        this.interfaceRepository = interfaceRepository;
        this.mappingTemplateRepository = mappingTemplateRepository;
    }

    public DashboardSummaryDto getSummary()
    {
        long configuredMandators = mandatorRepository.countMandatorsWithAtLeastOneCompany();
        long configuredInterfaces = interfaceRepository.countInterfacesWithAnyTemplate();
        long configuredMappingTemplates = mappingTemplateRepository.count();
        return new DashboardSummaryDto(configuredMandators, configuredInterfaces, configuredMappingTemplates);
    }
}
