package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.CompanyMappingDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyMappingSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.MappingSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.ResolvedMappingRowDto;
import com.smit.integrationhubmiddlelayer.exception.DuplicateCompanyMappingException;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.exception.InterfaceHasNoMappingTemplateException;
import com.smit.integrationhubmiddlelayer.service.CompanyMappingResolutionService;
import com.smit.integrationhubmiddlelayer.service.CompanyMappingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(CompanyMappingController.class)
@Import(GlobalExceptionHandler.class)
class CompanyMappingControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyMappingService companyMappingService;

    @MockitoBean
    private CompanyMappingResolutionService resolutionService;

    @Test
    void list_returns200WithBody() throws Exception
    {
        when(companyMappingService.list(10L)).thenReturn(
                List.of(new CompanyMappingSummaryDto(1L, 1L, "Volvo Grip Api", Instant.now()))); //$NON-NLS-1$

        mockMvc.perform(get("/companies/10/mappings")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].interfaceName").value("Volvo Grip Api")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns200WithBody() throws Exception
    {
        when(companyMappingService.create(eq(10L), any())).thenReturn(new CompanyMappingDetailDto(1L, 1L, "Volvo Grip Api", List.of())); //$NON-NLS-1$

        mockMvc.perform(post("/companies/10/mappings") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interfaceId\":1}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interfaceName").value("Volvo Grip Api")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns400_whenInterfaceHasNoMappingTemplate() throws Exception
    {
        when(companyMappingService.create(eq(10L), any())).thenThrow(new InterfaceHasNoMappingTemplateException(1L));

        mockMvc.perform(post("/companies/10/mappings") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interfaceId\":1}")) //$NON-NLS-1$
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INTERFACE_HAS_NO_MAPPING_TEMPLATE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns409_whenAlreadyExists() throws Exception
    {
        when(companyMappingService.create(eq(10L), any())).thenThrow(new DuplicateCompanyMappingException(10L, 1L));

        mockMvc.perform(post("/companies/10/mappings") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interfaceId\":1}")) //$NON-NLS-1$
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_COMPANY_MAPPING")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void setRowOverride_returns200WithBody() throws Exception
    {
        when(resolutionService.setRowOverride(eq(1L), eq(5L), eq("AT"))) //$NON-NLS-1$
                .thenReturn(new ResolvedMappingRowDto(5L, "Market", "AT", MappingSourceLevel.OVERRIDE, 1L)); //$NON-NLS-1$ //$NON-NLS-2$

        mockMvc.perform(put("/companies/10/mappings/1/rows/5") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"AT\"}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceLevel").value("OVERRIDE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void deleteRowOverride_returns200WithTemplateSourceLevel() throws Exception
    {
        when(resolutionService.deleteRowOverride(1L, 5L))
                .thenReturn(new ResolvedMappingRowDto(5L, "Market", "AT", MappingSourceLevel.TEMPLATE, null)); //$NON-NLS-1$ //$NON-NLS-2$

        mockMvc.perform(delete("/companies/10/mappings/1/rows/5")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceLevel").value("TEMPLATE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform(delete("/companies/10/mappings/1")) //$NON-NLS-1$
                .andExpect(status().isNoContent());
    }
}
