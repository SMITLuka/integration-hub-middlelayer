package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.DashboardSummaryDto;
import com.smit.integrationhubmiddlelayer.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard") //$NON-NLS-1$
public class DashboardController
{
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService)
    {
        this.dashboardService = dashboardService;
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<DashboardSummaryDto> getSummary()
    {
        log.info("GET /dashboard/summary"); //$NON-NLS-1$
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
