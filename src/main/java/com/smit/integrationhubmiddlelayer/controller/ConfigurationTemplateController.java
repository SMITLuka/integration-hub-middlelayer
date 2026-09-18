package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.service.ConfigurationTemplateService;
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
@RequestMapping("/interfaces/{id}/configuration-template") //$NON-NLS-1$
public class ConfigurationTemplateController
{
    private static final Logger log = LoggerFactory.getLogger(ConfigurationTemplateController.class);

    private final ConfigurationTemplateService configurationTemplateService;

    public ConfigurationTemplateController(ConfigurationTemplateService configurationTemplateService)
    {
        this.configurationTemplateService = configurationTemplateService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConfigurationTemplateDto> get(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("GET /interfaces/{}/configuration-template", id); //$NON-NLS-1$
        return ResponseEntity.ok(configurationTemplateService.get(id));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConfigurationTemplateDto> create(@PathVariable("id") Long id, //$NON-NLS-1$
            @Valid @RequestBody ConfigurationTemplateUpsertRequest request)
    {
        log.info("POST /interfaces/{}/configuration-template", id); //$NON-NLS-1$
        return ResponseEntity.ok(configurationTemplateService.create(id, request));
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConfigurationTemplateDto> replace(@PathVariable("id") Long id, //$NON-NLS-1$
            @Valid @RequestBody ConfigurationTemplateUpsertRequest request)
    {
        log.info("PUT /interfaces/{}/configuration-template", id); //$NON-NLS-1$
        return ResponseEntity.ok(configurationTemplateService.replace(id, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) //$NON-NLS-1$
    {
        log.info("DELETE /interfaces/{}/configuration-template", id); //$NON-NLS-1$
        configurationTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
