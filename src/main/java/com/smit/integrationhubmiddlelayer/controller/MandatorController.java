package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.dto.AdditionalDataValueRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorUpdateRequest;
import com.smit.integrationhubmiddlelayer.service.MandatorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mandators") //$NON-NLS-1$
public class MandatorController
{
    private static final Logger log = LoggerFactory.getLogger(MandatorController.class);

    private final MandatorService mandatorService;

    public MandatorController(MandatorService mandatorService)
    {
        this.mandatorService = mandatorService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MandatorSummaryDto>> list(
            @RequestParam(value = "search", required = false) String search, //$NON-NLS-1$
            @PageableDefault(size = 20) Pageable pageable)
    {
        log.info("GET /mandators - search: {}", search); //$NON-NLS-1$
        return ResponseEntity.ok(mandatorService.list(search, pageable));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MandatorDetailDto> create(@Valid @RequestBody MandatorCreateRequest request)
    {
        log.info("POST /mandators - name: {}", request.getName()); //$NON-NLS-1$
        return ResponseEntity.ok(mandatorService.create(request));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<MandatorDetailDto> getDetail(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("GET /mandators/{}", id); //$NON-NLS-1$
        return ResponseEntity.ok(mandatorService.getDetail(id));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<MandatorDetailDto> update(@PathVariable("id") Long id, //$NON-NLS-1$
            @Valid @RequestBody MandatorUpdateRequest request)
    {
        log.info("PUT /mandators/{}", id); //$NON-NLS-1$
        return ResponseEntity.ok(mandatorService.update(id, request));
    }

    @DeleteMapping("/{id}") //$NON-NLS-1$
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("DELETE /mandators/{}", id); //$NON-NLS-1$
        mandatorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/additional-data", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<List<AdditionalDataEntryDto>> getAdditionalData(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        return ResponseEntity.ok(mandatorService.getAdditionalData(id));
    }

    @PutMapping(value = "/{id}/additional-data/{key}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<AdditionalDataEntryDto> setAdditionalData(@PathVariable("id") Long id, //$NON-NLS-1$
            @PathVariable("key") String key, @Valid @RequestBody AdditionalDataValueRequest request) //$NON-NLS-1$
    {
        log.info("PUT /mandators/{}/additional-data/{}", id, key); //$NON-NLS-1$
        return ResponseEntity.ok(mandatorService.setAdditionalData(id, key, request.getValue()));
    }

    @DeleteMapping("/{id}/additional-data/{key}") //$NON-NLS-1$
    public ResponseEntity<Void> deleteAdditionalData(@PathVariable("id") Long id, @PathVariable("key") String key) //$NON-NLS-1$ //$NON-NLS-2$
    {
        log.info("DELETE /mandators/{}/additional-data/{}", id, key); //$NON-NLS-1$
        mandatorService.deleteAdditionalData(id, key);
        return ResponseEntity.noContent().build();
    }
}
