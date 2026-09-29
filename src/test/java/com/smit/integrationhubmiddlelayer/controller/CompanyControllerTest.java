package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.config.CorsConfig;
import com.smit.integrationhubmiddlelayer.config.SecurityConfig;
import com.smit.integrationhubmiddlelayer.dto.AdditionalDataSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.CompanyDetailDto;
import com.smit.integrationhubmiddlelayer.dto.ResolvedAdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.exception.AdditionalDataKeyNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.CompanyNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateCompanyException;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.service.CompanyAdditionalDataService;
import com.smit.integrationhubmiddlelayer.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CorsConfig.class})
@WithMockUser(roles = "EMPLOYEE") //$NON-NLS-1$
class CompanyControllerTest
{
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final UUID COMPANY_UUID = UUID.fromString("3c9e8f1a-2b4d-4e6f-8a1c-5d7e9f0a1b2c"); //$NON-NLS-1$

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyAdditionalDataService companyAdditionalDataService;

    private static CompanyDetailDto detail(Long id)
    {
        return new CompanyDetailDto(id, COMPANY_UUID, 1L, "Autohaus Rath GmbH", "Taferner - 10472", "1", "Vienna", "Slavonska avenija 11d", "AT", "de_AT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
                List.of(), List.of());
    }

    @Test
    void create_returns200WithBody() throws Exception
    {
        when(companyService.create(eq(1L), any())).thenReturn(detail(10L));

        mockMvc.perform(post("/mandators/1/companies") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Taferner - 10472\"}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Taferner - 10472")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.address").value("Slavonska avenija 11d")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.uuid").value(COMPANY_UUID.toString())) //$NON-NLS-1$
                .andExpect(jsonPath("$.customerNumber").doesNotExist()); //$NON-NLS-1$
    }

    @Test
    void create_returns409_whenDmsCompanyIdDuplicate() throws Exception
    {
        when(companyService.create(eq(1L), any())).thenThrow(new DuplicateCompanyException(1L, "1")); //$NON-NLS-1$

        mockMvc.perform(post("/mandators/1/companies") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Taferner - 10472\",\"dmsCompanyId\":\"1\"}")) //$NON-NLS-1$
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_COMPANY")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void getDetail_returns404_whenNotFound() throws Exception
    {
        when(companyService.getDetail(99L)).thenThrow(new CompanyNotFoundException(99L));

        mockMvc.perform(get("/companies/99")) //$NON-NLS-1$
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMPANY_NOT_FOUND")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void update_returns200WithBody() throws Exception
    {
        when(companyService.update(eq(10L), any())).thenReturn(detail(10L));

        mockMvc.perform(put("/companies/10") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Taferner - 10472\"}")) //$NON-NLS-1$
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform(delete("/companies/10")) //$NON-NLS-1$
                .andExpect(status().isNoContent());
    }

    @Test
    void getAdditionalData_returns200WithResolvedSourceLevels() throws Exception
    {
        when(companyAdditionalDataService.resolve(10L)).thenReturn(List.of(
                new ResolvedAdditionalDataEntryDto("PARTNER_TOKEN", "abc123", AdditionalDataSourceLevel.COMPANY), //$NON-NLS-1$ //$NON-NLS-2$
                new ResolvedAdditionalDataEntryDto("DMS_DB_USER", "ALEXANDER", AdditionalDataSourceLevel.MANDATOR))); //$NON-NLS-1$ //$NON-NLS-2$

        mockMvc.perform(get("/companies/10/additional-data")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceLevel").value("COMPANY")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$[1].sourceLevel").value("MANDATOR")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void setAdditionalData_returns200WithCompanySourceLevel() throws Exception
    {
        when(companyAdditionalDataService.setOverride(eq(10L), eq("PARTNER_TOKEN"), anyString())) //$NON-NLS-1$
                .thenReturn(new ResolvedAdditionalDataEntryDto("PARTNER_TOKEN", "xyz", AdditionalDataSourceLevel.COMPANY)); //$NON-NLS-1$ //$NON-NLS-2$

        mockMvc.perform(put("/companies/10/additional-data/PARTNER_TOKEN") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"xyz\"}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceLevel").value("COMPANY")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void deleteAdditionalData_returns404_whenKeyNotOverriddenAtCompanyLevel() throws Exception
    {
        doThrow(new AdditionalDataKeyNotFoundException("DMS_DB_USER")) //$NON-NLS-1$
                .when(companyAdditionalDataService).deleteOverride(10L, "DMS_DB_USER"); //$NON-NLS-1$

        mockMvc.perform(delete("/companies/10/additional-data/DMS_DB_USER")) //$NON-NLS-1$
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ADDITIONAL_DATA_KEY_NOT_FOUND")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
