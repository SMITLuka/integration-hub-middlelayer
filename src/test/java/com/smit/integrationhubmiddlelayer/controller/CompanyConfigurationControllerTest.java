package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.config.CorsConfig;
import com.smit.integrationhubmiddlelayer.config.PermissionPolicy;
import com.smit.integrationhubmiddlelayer.config.SecurityConfig;
import com.smit.integrationhubmiddlelayer.config.WithFullAccess;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.ResolvedConfigEntryDto;
import com.smit.integrationhubmiddlelayer.entity.ConfigValueType;
import com.smit.integrationhubmiddlelayer.exception.DuplicateConfigurationException;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.exception.InterfaceHasNoConfigurationTemplateException;
import com.smit.integrationhubmiddlelayer.service.CompanyConfigurationService;
import com.smit.integrationhubmiddlelayer.service.ConfigurationResolutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyConfigurationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CorsConfig.class, PermissionPolicy.class})
@WithFullAccess
class CompanyConfigurationControllerTest
{
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyConfigurationService companyConfigurationService;

    @MockitoBean
    private ConfigurationResolutionService resolutionService;

    @Test
    void list_returns200WithBody() throws Exception
    {
        when(companyConfigurationService.list(10L)).thenReturn(
                List.of(new CompanyConfigurationSummaryDto(1L, 1L, "Volvo Grip Api", Instant.now()))); //$NON-NLS-1$

        mockMvc.perform(get("/companies/10/configurations")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].interfaceName").value("Volvo Grip Api")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns200WithBody() throws Exception
    {
        when(companyConfigurationService.create(eq(10L), any()))
                .thenReturn(new CompanyConfigurationDetailDto(1L, 1L, "Volvo Grip Api", List.of())); //$NON-NLS-1$

        mockMvc.perform(post("/companies/10/configurations") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interfaceId\":1}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interfaceName").value("Volvo Grip Api")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns400_whenInterfaceHasNoConfigurationTemplate() throws Exception
    {
        when(companyConfigurationService.create(eq(10L), any())).thenThrow(new InterfaceHasNoConfigurationTemplateException(1L));

        mockMvc.perform(post("/companies/10/configurations") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interfaceId\":1}")) //$NON-NLS-1$
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INTERFACE_HAS_NO_CONFIGURATION_TEMPLATE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns409_whenAlreadyExists() throws Exception
    {
        when(companyConfigurationService.create(eq(10L), any())).thenThrow(new DuplicateConfigurationException(10L, 1L));

        mockMvc.perform(post("/companies/10/configurations") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interfaceId\":1}")) //$NON-NLS-1$
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CONFIGURATION")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void setEntryOverride_returns200WithBody() throws Exception
    {
        when(resolutionService.setOverride(eq(1L), eq(5L), eq("AT"))) //$NON-NLS-1$
                .thenReturn(new ResolvedConfigEntryDto(5L, "Market", ConfigValueType.TEXT, "desc", "AT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ConfigSourceLevel.OVERRIDE, 1L));

        mockMvc.perform(put("/companies/10/configurations/1/entries/5") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"AT\"}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceLevel").value("OVERRIDE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void deleteEntryOverride_returns200WithFallbackSourceLevel() throws Exception
    {
        when(resolutionService.deleteOverride(1L, 5L))
                .thenReturn(new ResolvedConfigEntryDto(5L, "Market", ConfigValueType.TEXT, "desc", "AT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ConfigSourceLevel.TEMPLATE, null));

        mockMvc.perform(delete("/companies/10/configurations/1/entries/5")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceLevel").value("TEMPLATE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform(delete("/companies/10/configurations/1")) //$NON-NLS-1$
                .andExpect(status().isNoContent());
    }
}
