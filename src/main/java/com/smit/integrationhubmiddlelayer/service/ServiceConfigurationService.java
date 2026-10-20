package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.ResolvedAdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.dto.ResolvedConfigEntryDto;
import com.smit.integrationhubmiddlelayer.dto.ServiceConfigurationDto;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.CompanyConfiguration;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.entity.Mandator;
import com.smit.integrationhubmiddlelayer.exception.ServiceLookupNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import com.smit.integrationhubmiddlelayer.repository.MandatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only lookup used by other services: finds a Company by the identifiers its DMS uses
 * (Mandator ID + Company ID) and returns the effective configuration of one Interface.
 */
@Service
@Transactional(readOnly = true)
public class ServiceConfigurationService
{
    private static final Logger log = LoggerFactory.getLogger(ServiceConfigurationService.class);

    private final MandatorRepository mandatorRepository;
    private final InterfaceRepository interfaceRepository;
    private final CompanyConfigurationRepository companyConfigurationRepository;
    private final ConfigurationResolutionService resolutionService;
    private final CompanyAdditionalDataService additionalDataService;

    public ServiceConfigurationService(MandatorRepository mandatorRepository, InterfaceRepository interfaceRepository,
            CompanyConfigurationRepository companyConfigurationRepository, ConfigurationResolutionService resolutionService,
            CompanyAdditionalDataService additionalDataService)
    {
        this.mandatorRepository = mandatorRepository;
        this.interfaceRepository = interfaceRepository;
        this.companyConfigurationRepository = companyConfigurationRepository;
        this.resolutionService = resolutionService;
        this.additionalDataService = additionalDataService;
    }

    public ServiceConfigurationDto resolve(String externalMandatorId, String dmsCompanyId, String interfaceName)
    {
        Mandator mandator = findMandator(externalMandatorId);
        Company company = findCompany(mandator, dmsCompanyId);
        Interface interfaceEntity = findInterface(interfaceName);

        CompanyConfiguration configuration = companyConfigurationRepository
                .findByCompanyIdAndInterfaceEntityId(company.getId(), interfaceEntity.getId())
                .orElseThrow(() -> new ServiceLookupNotFoundException("CONFIGURATION_NOT_FOUND", //$NON-NLS-1$
                        "Company '" + company.getName() + "' has no configuration for interface '" + interfaceEntity.getName() + "'")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        Map<String, String> config = new LinkedHashMap<>();
        for (ResolvedConfigEntryDto entry : resolutionService.resolveEffectiveConfig(configuration.getId()))
        {
            config.put(entry.getKey(), entry.getEffectiveValue());
        }

        Map<String, String> additionalData = new TreeMap<>();
        for (ResolvedAdditionalDataEntryDto entry : additionalDataService.resolve(company.getId()))
        {
            additionalData.put(entry.getKey(), entry.getValue());
        }

        log.info("Service lookup mandator={} company={} interface='{}' -> configurationId={}, {} keys", //$NON-NLS-1$
                mandator.getExternalMandatorId(), company.getDmsCompanyId(), interfaceEntity.getName(), configuration.getId(), config.size());

        return new ServiceConfigurationDto(
                new ServiceConfigurationDto.MandatorInfo(String.valueOf(mandator.getUuid()), mandator.getName(),
                        mandator.getExternalMandatorId(), mandator.getSystem(), mandator.getPersonalIdentificationNumber(),
                        mandator.getHostUrl(), mandator.getPort()),
                new ServiceConfigurationDto.CompanyInfo(String.valueOf(company.getUuid()), company.getName(),
                        company.getDmsCompanyId(), company.getLocation(), company.getAddress()),
                interfaceEntity.getName(), config, additionalData);
    }

    private Mandator findMandator(String externalMandatorId)
    {
        String wanted = externalMandatorId == null ? "" : externalMandatorId.trim(); //$NON-NLS-1$
        return mandatorRepository.findByExternalMandatorIdIgnoreCase(wanted)
                .orElseThrow(() -> new ServiceLookupNotFoundException("MANDATOR_NOT_FOUND", //$NON-NLS-1$
                        "No mandator with Mandator ID '" + wanted + "'")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** DMS systems pad company numbers ("000002"), the Hub may store them unpadded ("2"): compare without leading zeros. */
    private Company findCompany(Mandator mandator, String dmsCompanyId)
    {
        String wanted = normalizeCompanyId(dmsCompanyId);
        List<Company> matches = mandator.getCompanies().stream()
                .filter(c -> normalizeCompanyId(c.getDmsCompanyId()).equals(wanted))
                .toList();
        if (matches.isEmpty())
        {
            throw new ServiceLookupNotFoundException("COMPANY_NOT_FOUND", //$NON-NLS-1$
                    "Mandator '" + mandator.getExternalMandatorId() + "' has no company with Company ID '" + dmsCompanyId + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        return matches.get(0);
    }

    private Interface findInterface(String interfaceName)
    {
        String wanted = interfaceName == null ? "" : interfaceName.trim(); //$NON-NLS-1$
        return interfaceRepository.findByNameIgnoreCase(wanted)
                .orElseThrow(() -> new ServiceLookupNotFoundException("INTERFACE_NOT_FOUND", //$NON-NLS-1$
                        "No interface named '" + wanted + "'")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    static String normalizeCompanyId(String value)
    {
        if (value == null)
        {
            return ""; //$NON-NLS-1$
        }
        String trimmed = value.trim();
        String stripped = trimmed.replaceFirst("^0+(?=.)", ""); //$NON-NLS-1$ //$NON-NLS-2$
        return stripped.toUpperCase();
    }
}
