package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorSummaryDto;
import com.smit.integrationhubmiddlelayer.exception.DuplicateMandatorException;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.exception.MandatorHasCompaniesException;
import com.smit.integrationhubmiddlelayer.exception.MandatorNotFoundException;
import com.smit.integrationhubmiddlelayer.service.MandatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MandatorController.class)
@Import(GlobalExceptionHandler.class)
class MandatorControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MandatorService mandatorService;

    private static MandatorDetailDto detail(Long id)
    {
        return new MandatorDetailDto(id, "Autohaus Rath GmbH", "MD.DMS", "customer", "MD_10386", "AT", "de_AT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                List.of(new AdditionalDataEntryDto("DMS_DB_USER", "ALEXANDER")), List.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void list_returns200WithPage() throws Exception
    {
        MandatorSummaryDto summary = new MandatorSummaryDto(1L, "Autohaus Rath GmbH", "MD.DMS", "customer", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "MD_10386", "AT", "de_AT", 1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        when(mandatorService.list(isNull(), any())).thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/mandators")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Autohaus Rath GmbH")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns200WithBody() throws Exception
    {
        when(mandatorService.create(any())).thenReturn(detail(1L));

        mockMvc.perform(post("/mandators") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Autohaus Rath GmbH\",\"externalMandatorId\":\"MD_10386\"}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Autohaus Rath GmbH")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns400_whenNameBlank() throws Exception
    {
        mockMvc.perform(post("/mandators") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")) //$NON-NLS-1$
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns409_whenExternalMandatorIdDuplicate() throws Exception
    {
        when(mandatorService.create(any())).thenThrow(new DuplicateMandatorException("MD_10386")); //$NON-NLS-1$

        mockMvc.perform(post("/mandators") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Autohaus Rath GmbH\",\"externalMandatorId\":\"MD_10386\"}")) //$NON-NLS-1$
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_MANDATOR")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void getDetail_returns200WithBody_whenFound() throws Exception
    {
        when(mandatorService.getDetail(1L)).thenReturn(detail(1L));

        mockMvc.perform(get("/mandators/1")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalData[0].key").value("DMS_DB_USER")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void getDetail_returns404_whenNotFound() throws Exception
    {
        when(mandatorService.getDetail(99L)).thenThrow(new MandatorNotFoundException(99L));

        mockMvc.perform(get("/mandators/99")) //$NON-NLS-1$
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MANDATOR_NOT_FOUND")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void update_returns200WithBody() throws Exception
    {
        when(mandatorService.update(eq(1L), any())).thenReturn(detail(1L));

        mockMvc.perform(put("/mandators/1") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Autohaus Rath GmbH\"}")) //$NON-NLS-1$
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204_whenNoCompanies() throws Exception
    {
        mockMvc.perform(delete("/mandators/1")) //$NON-NLS-1$
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns409_whenMandatorHasCompanies() throws Exception
    {
        doThrow(new MandatorHasCompaniesException(1L)).when(mandatorService).delete(1L);

        mockMvc.perform(delete("/mandators/1")) //$NON-NLS-1$
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MANDATOR_HAS_COMPANIES")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void setAdditionalData_returns200WithBody() throws Exception
    {
        when(mandatorService.setAdditionalData(eq(1L), eq("PARTNER_TOKEN"), anyString())) //$NON-NLS-1$
                .thenReturn(new AdditionalDataEntryDto("PARTNER_TOKEN", "abc123")); //$NON-NLS-1$ //$NON-NLS-2$

        mockMvc.perform(put("/mandators/1/additional-data/PARTNER_TOKEN") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"abc123\"}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("abc123")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void deleteAdditionalData_returns204() throws Exception
    {
        mockMvc.perform(delete("/mandators/1/additional-data/PARTNER_TOKEN")) //$NON-NLS-1$
                .andExpect(status().isNoContent());
    }
}
