package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.ResolvedConfigEntryDto;
import com.smit.integrationhubmiddlelayer.dto.RowValueRequest;
import com.smit.integrationhubmiddlelayer.service.CompanyConfigurationService;
import com.smit.integrationhubmiddlelayer.service.ConfigurationResolutionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/companies/{companyId}/configurations") //$NON-NLS-1$
public class CompanyConfigurationController
{
    private static final Logger log = LoggerFactory.getLogger(CompanyConfigurationController.class);

    private final CompanyConfigurationService companyConfigurationService;
    private final ConfigurationResolutionService resolutionService;

    public CompanyConfigurationController(CompanyConfigurationService companyConfigurationService,
            ConfigurationResolutionService resolutionService)
    {
        this.companyConfigurationService = companyConfigurationService;
        this.resolutionService = resolutionService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CompanyConfigurationSummaryDto>> list(@PathVariable("companyId") Long companyId) //$NON-NLS-1$
    {
        log.info("GET /companies/{}/configurations", companyId); //$NON-NLS-1$
        return ResponseEntity.ok(companyConfigurationService.list(companyId));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanyConfigurationDetailDto> create(@PathVariable("companyId") Long companyId, //$NON-NLS-1$
            @Valid @RequestBody CompanyConfigurationCreateRequest request)
    {
        log.info("POST /companies/{}/configurations - interfaceId: {}", companyId, request.getInterfaceId()); //$NON-NLS-1$
        return ResponseEntity.ok(companyConfigurationService.create(companyId, request));
    }

    @GetMapping(value = "/{configId}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<CompanyConfigurationDetailDto> getDetail(@PathVariable("companyId") Long companyId, //$NON-NLS-1$
            @PathVariable("configId") Long configId) //$NON-NLS-1$
    {
        log.info("GET /companies/{}/configurations/{}", companyId, configId); //$NON-NLS-1$
        return ResponseEntity.ok(companyConfigurationService.getDetail(companyId, configId));
    }

    @PutMapping(value = "/{configId}/entries/{templateEntryId}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<ResolvedConfigEntryDto> setEntryOverride(@PathVariable("companyId") Long companyId, //$NON-NLS-1$
            @PathVariable("configId") Long configId, @PathVariable("templateEntryId") Long templateEntryId, //$NON-NLS-1$ //$NON-NLS-2$
            @Valid @RequestBody RowValueRequest request)
    {
        log.info("PUT /companies/{}/configurations/{}/entries/{}", companyId, configId, templateEntryId); //$NON-NLS-1$
        return ResponseEntity.ok(resolutionService.setOverride(configId, templateEntryId, request.getValue()));
    }

    @DeleteMapping(value = "/{configId}/entries/{templateEntryId}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<ResolvedConfigEntryDto> deleteEntryOverride(@PathVariable("companyId") Long companyId, //$NON-NLS-1$
            @PathVariable("configId") Long configId, @PathVariable("templateEntryId") Long templateEntryId) //$NON-NLS-1$ //$NON-NLS-2$
    {
        log.info("DELETE /companies/{}/configurations/{}/entries/{}", companyId, configId, templateEntryId); //$NON-NLS-1$
        return ResponseEntity.ok(resolutionService.deleteOverride(configId, templateEntryId));
    }

    @DeleteMapping("/{configId}") //$NON-NLS-1$
    public ResponseEntity<Void> delete(@PathVariable("companyId") Long companyId, @PathVariable("configId") Long configId) //$NON-NLS-1$ //$NON-NLS-2$
    {
        log.info("DELETE /companies/{}/configurations/{}", companyId, configId); //$NON-NLS-1$
        companyConfigurationService.delete(companyId, configId);
        return ResponseEntity.noContent().build();
    }
}
