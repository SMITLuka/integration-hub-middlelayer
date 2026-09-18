package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.MappingTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.service.MappingTemplateService;
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

@RestController
@RequestMapping("/interfaces/{id}/mapping-template") //$NON-NLS-1$
public class MappingTemplateController
{
    private static final Logger log = LoggerFactory.getLogger(MappingTemplateController.class);

    private final MappingTemplateService mappingTemplateService;

    public MappingTemplateController(MappingTemplateService mappingTemplateService)
    {
        this.mappingTemplateService = mappingTemplateService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MappingTemplateDto> get(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("GET /interfaces/{}/mapping-template", id); //$NON-NLS-1$
        return ResponseEntity.ok(mappingTemplateService.get(id));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MappingTemplateDto> create(@PathVariable("id") Long id, //$NON-NLS-1$
            @Valid @RequestBody MappingTemplateUpsertRequest request)
    {
        log.info("POST /interfaces/{}/mapping-template", id); //$NON-NLS-1$
        return ResponseEntity.ok(mappingTemplateService.create(id, request));
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MappingTemplateDto> replace(@PathVariable("id") Long id, //$NON-NLS-1$
            @Valid @RequestBody MappingTemplateUpsertRequest request)
    {
        log.info("PUT /interfaces/{}/mapping-template", id); //$NON-NLS-1$
        return ResponseEntity.ok(mappingTemplateService.replace(id, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("DELETE /interfaces/{}/mapping-template", id); //$NON-NLS-1$
        mappingTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
