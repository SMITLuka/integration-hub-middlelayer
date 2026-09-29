package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.dto.CompanySummaryDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorUpdateRequest;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.Mandator;
import com.smit.integrationhubmiddlelayer.exception.AdditionalDataKeyNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateMandatorException;
import com.smit.integrationhubmiddlelayer.exception.MandatorHasCompaniesException;
import com.smit.integrationhubmiddlelayer.exception.MandatorNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.MandatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * CRUD and Additional Data management for Mandators.
 */
@Service
@Transactional
public class MandatorService
{
    private static final Logger log = LoggerFactory.getLogger(MandatorService.class);

    private final MandatorRepository mandatorRepository;

    public MandatorService(MandatorRepository mandatorRepository)
    {
        this.mandatorRepository = mandatorRepository;
    }

    @Transactional(readOnly = true)
    public Page<MandatorSummaryDto> list(String search, Pageable pageable)
    {
        Page<Mandator> page = StringUtils.hasText(search)
                ? mandatorRepository.findByNameContainingIgnoreCase(search, pageable)
                : mandatorRepository.findAll(pageable);
        return page.map(this::toSummaryDto);
    }

    public MandatorDetailDto create(MandatorCreateRequest request)
    {
        String externalMandatorId = normalizeExternalMandatorId(request.getExternalMandatorId());
        if (externalMandatorId != null && mandatorRepository.findByExternalMandatorId(externalMandatorId).isPresent())
        {
            throw new DuplicateMandatorException(externalMandatorId);
        }

        Mandator mandator = Mandator.builder()
                .name(request.getName())
                .system(request.getSystem())
                .personalIdentificationNumber(request.getPersonalIdentificationNumber())
                .externalMandatorId(externalMandatorId)
                .hostUrl(request.getHostUrl())
                .port(request.getPort())
                .country(request.getCountry())
                .locale(request.getLocale())
                .build();

        Mandator saved = mandatorRepository.save(mandator);
        log.info("Created mandator id={}", saved.getId()); //$NON-NLS-1$
        return toDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public MandatorDetailDto getDetail(Long mandatorId)
    {
        return toDetailDto(findOrThrow(mandatorId));
    }

    public MandatorDetailDto update(Long mandatorId, MandatorUpdateRequest request)
    {
        Mandator mandator = findOrThrow(mandatorId);
        String externalMandatorId = normalizeExternalMandatorId(request.getExternalMandatorId());
        if (externalMandatorId != null)
        {
            mandatorRepository.findByExternalMandatorId(externalMandatorId)
                    .filter(existing -> !existing.getId().equals(mandatorId))
                    .ifPresent(existing ->
                    {
                        throw new DuplicateMandatorException(externalMandatorId);
                    });
        }

        mandator.setName(request.getName());
        mandator.setSystem(request.getSystem());
        mandator.setPersonalIdentificationNumber(request.getPersonalIdentificationNumber());
        mandator.setExternalMandatorId(externalMandatorId);
        mandator.setHostUrl(request.getHostUrl());
        mandator.setPort(request.getPort());
        mandator.setCountry(request.getCountry());
        mandator.setLocale(request.getLocale());
        log.info("Updated mandator id={}", mandatorId); //$NON-NLS-1$
        return toDetailDto(mandator);
    }

    public void delete(Long mandatorId)
    {
        Mandator mandator = findOrThrow(mandatorId);
        if (!mandator.getCompanies().isEmpty())
        {
            throw new MandatorHasCompaniesException(mandatorId);
        }
        mandatorRepository.delete(mandator);
        log.info("Deleted mandator id={}", mandatorId); //$NON-NLS-1$
    }

    @Transactional(readOnly = true)
    public List<AdditionalDataEntryDto> getAdditionalData(Long mandatorId)
    {
        return findOrThrow(mandatorId).getAdditionalData().entrySet().stream()
                .map(entry -> new AdditionalDataEntryDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    public AdditionalDataEntryDto setAdditionalData(Long mandatorId, String key, String value)
    {
        Mandator mandator = findOrThrow(mandatorId);
        mandator.getAdditionalData().put(key, value);
        log.info("Set mandator additional data id={} key={}", mandatorId, key); //$NON-NLS-1$
        return new AdditionalDataEntryDto(key, value);
    }

    public void deleteAdditionalData(Long mandatorId, String key)
    {
        Mandator mandator = findOrThrow(mandatorId);
        if (mandator.getAdditionalData().remove(key) == null)
        {
            throw new AdditionalDataKeyNotFoundException(key);
        }
        log.info("Deleted mandator additional data id={} key={}", mandatorId, key); //$NON-NLS-1$
    }

    Mandator findOrThrow(Long mandatorId)
    {
        return mandatorRepository.findById(mandatorId).orElseThrow(() -> new MandatorNotFoundException(mandatorId));
    }

    /**
     * The external_mandator_id column is UNIQUE: PostgreSQL allows many NULLs there but only one
     * empty string, so a blank ID (the UI sends "" for an empty field) must be stored as NULL.
     */
    private static String normalizeExternalMandatorId(String externalMandatorId)
    {
        return StringUtils.hasText(externalMandatorId) ? externalMandatorId : null;
    }

    private MandatorSummaryDto toSummaryDto(Mandator mandator)
    {
        return new MandatorSummaryDto(mandator.getId(), mandator.getName(), mandator.getSystem(), mandator.getPersonalIdentificationNumber(),
                mandator.getExternalMandatorId(), mandator.getHostUrl(), mandator.getPort(), mandator.getCountry(), mandator.getLocale(),
                mandator.getCompanies().size());
    }

    private MandatorDetailDto toDetailDto(Mandator mandator)
    {
        List<AdditionalDataEntryDto> additionalData = mandator.getAdditionalData().entrySet().stream()
                .map(entry -> new AdditionalDataEntryDto(entry.getKey(), entry.getValue()))
                .toList();
        List<CompanySummaryDto> companies = mandator.getCompanies().stream()
                .map(this::toCompanySummaryDto)
                .toList();
        return new MandatorDetailDto(mandator.getId(), mandator.getName(), mandator.getSystem(), mandator.getPersonalIdentificationNumber(),
                mandator.getExternalMandatorId(), mandator.getHostUrl(), mandator.getPort(), mandator.getCountry(), mandator.getLocale(),
                additionalData, companies);
    }

    private CompanySummaryDto toCompanySummaryDto(Company company)
    {
        return new CompanySummaryDto(company.getId(), company.getName(), company.getDmsCompanyId(),
                company.getLocation(), company.getCountryCode());
    }
}
