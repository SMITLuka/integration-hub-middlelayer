package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.ServiceConfigurationDto;
import com.smit.integrationhubmiddlelayer.service.ServiceConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Machine-to-machine API for other SMIT services. Authenticated with an API key
 * (X-Api-Key, see ServiceApiSecurityConfig), not with an SSO user login.
 */
@RestController
@RequestMapping("/service") //$NON-NLS-1$
public class ServiceConfigurationController
{
    private static final Logger log = LoggerFactory.getLogger(ServiceConfigurationController.class);

    private final ServiceConfigurationService serviceConfigurationService;

    public ServiceConfigurationController(ServiceConfigurationService serviceConfigurationService)
    {
        this.serviceConfigurationService = serviceConfigurationService;
    }

    /**
     * Example: GET /service/configuration?mandatorId=B0&companyId=000001&interfaceName=MIMW%20integration
     */
    @GetMapping(value = "/configuration", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<ServiceConfigurationDto> configuration(@RequestParam("mandatorId") String mandatorId, //$NON-NLS-1$
            @RequestParam("companyId") String companyId, @RequestParam("interfaceName") String interfaceName) //$NON-NLS-1$ //$NON-NLS-2$
    {
        log.info("GET /service/configuration mandatorId={} companyId={} interfaceName='{}'", mandatorId, companyId, interfaceName); //$NON-NLS-1$
        return ResponseEntity.ok(serviceConfigurationService.resolve(mandatorId, companyId, interfaceName));
    }
}
