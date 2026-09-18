package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.ResolvedAdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.exception.AdditionalDataKeyNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.CompanyNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves a Company's effective Additional Data by overlaying its own overrides
 * on top of its parent Mandator's Additional Data (Company shadows Mandator).
 */
@Service
@Transactional
public class CompanyAdditionalDataService
{
    private static final Logger log = LoggerFactory.getLogger(CompanyAdditionalDataService.class);

    private final CompanyRepository companyRepository;

    public CompanyAdditionalDataService(CompanyRepository companyRepository)
    {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<ResolvedAdditionalDataEntryDto> resolve(Long companyId)
    {
        Company company = findOrThrow(companyId);
        Map<String, String> companyData = company.getAdditionalData();
        Map<String, String> mandatorData = company.getMandator().getAdditionalData();

        Set<String> keys = new HashSet<>(mandatorData.keySet());
        keys.addAll(companyData.keySet());

        return keys.stream()
                .map(key -> resolveEntry(key, companyData, mandatorData))
                .toList();
    }

    public ResolvedAdditionalDataEntryDto setOverride(Long companyId, String key, String value)
    {
        Company company = findOrThrow(companyId);
        company.getAdditionalData().put(key, value);
        log.info("Set company additional data override companyId={} key={}", companyId, key); //$NON-NLS-1$
        return new ResolvedAdditionalDataEntryDto(key, value, AdditionalDataSourceLevel.COMPANY);
    }

    public void deleteOverride(Long companyId, String key)
    {
        Company company = findOrThrow(companyId);
        if (company.getAdditionalData().remove(key) == null)
        {
            throw new AdditionalDataKeyNotFoundException(key);
        }
        log.info("Deleted company additional data override companyId={} key={}", companyId, key); //$NON-NLS-1$
    }

    private ResolvedAdditionalDataEntryDto resolveEntry(String key, Map<String, String> companyData, Map<String, String> mandatorData)
    {
        if (companyData.containsKey(key))
        {
            return new ResolvedAdditionalDataEntryDto(key, companyData.get(key), AdditionalDataSourceLevel.COMPANY);
        }
        return new ResolvedAdditionalDataEntryDto(key, mandatorData.get(key), AdditionalDataSourceLevel.MANDATOR);
    }

    private Company findOrThrow(Long companyId)
    {
        return companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    }
}
