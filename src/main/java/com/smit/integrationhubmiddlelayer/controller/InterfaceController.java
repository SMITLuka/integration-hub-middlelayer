package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.dto.AdditionalDataValueRequest;
import com.smit.integrationhubmiddlelayer.dto.InterfaceCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.InterfaceDetailDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceUpdateRequest;
import com.smit.integrationhubmiddlelayer.service.InterfaceService;
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
@RequestMapping("/interfaces") //$NON-NLS-1$
public class InterfaceController
{
    private static final Logger log = LoggerFactory.getLogger(InterfaceController.class);

    private final InterfaceService interfaceService;

    public InterfaceController(InterfaceService interfaceService)
    {
        this.interfaceService = interfaceService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<InterfaceSummaryDto>> list(
            @RequestParam(value = "search", required = false) String search, //$NON-NLS-1$
            @PageableDefault(size = 20) Pageable pageable)
    {
        log.info("GET /interfaces - search: {}", search); //$NON-NLS-1$
        return ResponseEntity.ok(interfaceService.list(search, pageable));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InterfaceDetailDto> create(@Valid @RequestBody InterfaceCreateRequest request)
    {
        log.info("POST /interfaces - name: {}", request.getName()); //$NON-NLS-1$
        return ResponseEntity.ok(interfaceService.create(request));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<InterfaceDetailDto> getDetail(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("GET /interfaces/{}", id); //$NON-NLS-1$
        return ResponseEntity.ok(interfaceService.getDetail(id));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<InterfaceDetailDto> update(@PathVariable("id") Long id, //$NON-NLS-1$
            @Valid @RequestBody InterfaceUpdateRequest request)
    {
        log.info("PUT /interfaces/{}", id); //$NON-NLS-1$
        return ResponseEntity.ok(interfaceService.update(id, request));
    }

    @DeleteMapping("/{id}") //$NON-NLS-1$
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("DELETE /interfaces/{}", id); //$NON-NLS-1$
        interfaceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/additional-data", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<List<AdditionalDataEntryDto>> getAdditionalData(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        return ResponseEntity.ok(interfaceService.getAdditionalData(id));
    }

    @PutMapping(value = "/{id}/additional-data/{key}", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<AdditionalDataEntryDto> setAdditionalData(@PathVariable("id") Long id, //$NON-NLS-1$
            @PathVariable("key") String key, @Valid @RequestBody AdditionalDataValueRequest request) //$NON-NLS-1$
    {
        log.info("PUT /interfaces/{}/additional-data/{}", id, key); //$NON-NLS-1$
        return ResponseEntity.ok(interfaceService.setAdditionalData(id, key, request.getValue()));
    }

    @DeleteMapping("/{id}/additional-data/{key}") //$NON-NLS-1$
    public ResponseEntity<Void> deleteAdditionalData(@PathVariable("id") Long id, @PathVariable("key") String key) //$NON-NLS-1$ //$NON-NLS-2$
    {
        log.info("DELETE /interfaces/{}/additional-data/{}", id, key); //$NON-NLS-1$
        interfaceService.deleteAdditionalData(id, key);
        return ResponseEntity.noContent().build();
    }
}
