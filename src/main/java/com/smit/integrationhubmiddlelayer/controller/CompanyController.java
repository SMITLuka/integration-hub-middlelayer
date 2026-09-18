package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataValueRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyUpdateRequest;
import com.smit.integrationhubmiddlelayer.dto.ResolvedAdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.service.CompanyAdditionalDataService;
import com.smit.integrationhubmiddlelayer.service.CompanyService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CompanyController
{
    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyService companyService;
    private final CompanyAdditionalDataService companyAdditionalDataService;

    public CompanyController(CompanyService companyService, CompanyAdditionalDataService companyAdditionalDataService)
    {
        this.companyService = companyService;
        this.companyAdditionalDataService = companyAdditionalDataService;
    }

    @PostMapping(value = "/mandators/{mandatorId}/companies", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<CompanyDetailDto> create(@PathVariable("mandatorId") Long mandatorId, //$NON-NLS-1$
            @Valid @RequestBody CompanyCreateRequest request)
    {
        log.info("POST /mandators/{}/companies - name: {}", mandatorId, request.getName()); //$NON-NLS-1$
        return ResponseEntity.ok(companyService.create(mandatorId, request));
    }

    @GetMapping(value = "/companies/{id}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<CompanyDetailDto> getDetail(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("GET /companies/{}", id); //$NON-NLS-1$
        return ResponseEntity.ok(companyService.getDetail(id));
    }

    @PutMapping(value = "/companies/{id}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<CompanyDetailDto> update(@PathVariable("id") Long id, @Valid @RequestBody CompanyUpdateRequest request) //$NON-NLS-1$
    {
        log.info("PUT /companies/{}", id); //$NON-NLS-1$
        return ResponseEntity.ok(companyService.update(id, request));
    }

    @DeleteMapping("/companies/{id}") //$NON-NLS-1$
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("DELETE /companies/{}", id); //$NON-NLS-1$
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/companies/{id}/additional-data", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<List<ResolvedAdditionalDataEntryDto>> getAdditionalData(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        return ResponseEntity.ok(companyAdditionalDataService.resolve(id));
    }

    @PutMapping(value = "/companies/{id}/additional-data/{key}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<ResolvedAdditionalDataEntryDto> setAdditionalData(@PathVariable("id") Long id, //$NON-NLS-1$
            @PathVariable("key") String key, @Valid @RequestBody AdditionalDataValueRequest request) //$NON-NLS-1$
    {
        log.info("PUT /companies/{}/additional-data/{}", id, key); //$NON-NLS-1$
        return ResponseEntity.ok(companyAdditionalDataService.setOverride(id, key, request.getValue()));
    }

    @DeleteMapping("/companies/{id}/additional-data/{key}") //$NON-NLS-1$
    public ResponseEntity<Void> deleteAdditionalData(@PathVariable("id") Long id, @PathVariable("key") String key) //$NON-NLS-1$ //$NON-NLS-2$
    {
        log.info("DELETE /companies/{}/additional-data/{}", id, key); //$NON-NLS-1$
        companyAdditionalDataService.deleteOverride(id, key);
        return ResponseEntity.noContent().build();
    }
}
