package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.CompanyMappingCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyMappingDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyMappingSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.ResolvedMappingRowDto;
import com.smit.integrationhubmiddlelayer.dto.RowValueRequest;
import com.smit.integrationhubmiddlelayer.service.CompanyMappingResolutionService;
import com.smit.integrationhubmiddlelayer.service.CompanyMappingService;
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
@RequestMapping("/companies/{companyId}/mappings") //$NON-NLS-1$
public class CompanyMappingController
{
    private static final Logger log = LoggerFactory.getLogger(CompanyMappingController.class);

    private final CompanyMappingService companyMappingService;
    private final CompanyMappingResolutionService resolutionService;

    public CompanyMappingController(CompanyMappingService companyMappingService, CompanyMappingResolutionService resolutionService)
    {
        this.companyMappingService = companyMappingService;
        this.resolutionService = resolutionService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CompanyMappingSummaryDto>> list(@PathVariable("companyId") Long companyId) //$NON-NLS-1$
    {
        log.info("GET /companies/{}/mappings", companyId); //$NON-NLS-1$
        return ResponseEntity.ok(companyMappingService.list(companyId));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanyMappingDetailDto> create(@PathVariable("companyId") Long companyId, //$NON-NLS-1$
            @Valid @RequestBody CompanyMappingCreateRequest request)
    {
        log.info("POST /companies/{}/mappings - interfaceId: {}", companyId, request.getInterfaceId()); //$NON-NLS-1$
        return ResponseEntity.ok(companyMappingService.create(companyId, request));
    }

    @GetMapping(value = "/{mappingId}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<CompanyMappingDetailDto> getDetail(@PathVariable("companyId") Long companyId, //$NON-NLS-1$
            @PathVariable("mappingId") Long mappingId) //$NON-NLS-1$
    {
        log.info("GET /companies/{}/mappings/{}", companyId, mappingId); //$NON-NLS-1$
        return ResponseEntity.ok(companyMappingService.getDetail(companyId, mappingId));
    }

    @PutMapping(value = "/{mappingId}/rows/{rowId}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<ResolvedMappingRowDto> setRowOverride(@PathVariable("companyId") Long companyId, //$NON-NLS-1$
            @PathVariable("mappingId") Long mappingId, @PathVariable("rowId") Long rowId, //$NON-NLS-1$ //$NON-NLS-2$
            @Valid @RequestBody RowValueRequest request)
    {
        log.info("PUT /companies/{}/mappings/{}/rows/{}", companyId, mappingId, rowId); //$NON-NLS-1$
        return ResponseEntity.ok(resolutionService.setRowOverride(mappingId, rowId, request.getValue()));
    }

    @DeleteMapping(value = "/{mappingId}/rows/{rowId}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<ResolvedMappingRowDto> deleteRowOverride(@PathVariable("companyId") Long companyId, //$NON-NLS-1$
            @PathVariable("mappingId") Long mappingId, @PathVariable("rowId") Long rowId) //$NON-NLS-1$ //$NON-NLS-2$
    {
        log.info("DELETE /companies/{}/mappings/{}/rows/{}", companyId, mappingId, rowId); //$NON-NLS-1$
        return ResponseEntity.ok(resolutionService.deleteRowOverride(mappingId, rowId));
    }

    @DeleteMapping("/{mappingId}") //$NON-NLS-1$
    public ResponseEntity<Void> delete(@PathVariable("companyId") Long companyId, @PathVariable("mappingId") Long mappingId) //$NON-NLS-1$ //$NON-NLS-2$
    {
        log.info("DELETE /companies/{}/mappings/{}", companyId, mappingId); //$NON-NLS-1$
        companyMappingService.delete(companyId, mappingId);
        return ResponseEntity.noContent().build();
    }
}
