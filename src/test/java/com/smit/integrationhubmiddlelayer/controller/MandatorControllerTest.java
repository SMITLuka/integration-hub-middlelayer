package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorSummaryDto;
import com.smit.integrationhubmiddlelayer.exception.DuplicateMandatorException;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.exception.MandatorHasCompaniesException;
import com.smit.integrationhubmiddlelayer.exception.MandatorNotFoundException;
import com.smit.integrationhubmiddlelayer.service.MandatorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
    private static final UUID MANDATOR_UUID = UUID.fromString("7b1f3c2e-8d4a-4f6b-9c1e-2a3b4c5d6e7f"); //$NON-NLS-1$

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MandatorService mandatorService;

    private static MandatorDetailDto detail(Long id)
    {
        return new MandatorDetailDto(id, MANDATOR_UUID, "Autohaus Rath GmbH", "MD.DMS", "12934449907", "MD_10386", "pantheon.sm-it.hr", 1666, "AT", "de_AT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
                List.of(new AdditionalDataEntryDto("DMS_DB_USER", "ALEXANDER")), List.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void list_returns200WithPage() throws Exception
    {
        MandatorSummaryDto summary = new MandatorSummaryDto(1L, MANDATOR_UUID, "Autohaus Rath GmbH", "MD.DMS", "12934449907", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "MD_10386", "pantheon.sm-it.hr", 1666, "AT", "de_AT", 1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
                        .content("{\"name\":\"Autohaus Rath GmbH\",\"externalMandatorId\":\"MD_10386\"," //$NON-NLS-1$
                                + "\"personalIdentificationNumber\":\"12934449907\",\"hostUrl\":\"pantheon.sm-it.hr\",\"port\":1666}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Autohaus Rath GmbH")); //$NON-NLS-1$ //$NON-NLS-2$

        ArgumentCaptor<MandatorCreateRequest> captor = ArgumentCaptor.forClass(MandatorCreateRequest.class);
        verify(mandatorService).create(captor.capture());
        assertThat(captor.getValue().getPersonalIdentificationNumber()).isEqualTo("12934449907"); //$NON-NLS-1$
        assertThat(captor.getValue().getHostUrl()).isEqualTo("pantheon.sm-it.hr"); //$NON-NLS-1$
        assertThat(captor.getValue().getPort()).isEqualTo(1666);
    }

    @Test
    void create_returns400_whenPortNotNumeric() throws Exception
    {
        mockMvc.perform(post("/mandators") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Autohaus Rath GmbH\",\"port\":\"abc\"}")) //$NON-NLS-1$
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST")); //$NON-NLS-1$ //$NON-NLS-2$
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
    void create_returns400_whenPortOutOfRange() throws Exception
    {
        mockMvc.perform(post("/mandators") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Autohaus Rath GmbH\",\"port\":65536}")) //$NON-NLS-1$
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void update_returns400_whenPortBelowRange() throws Exception
    {
        mockMvc.perform(put("/mandators/1") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Autohaus Rath GmbH\",\"port\":0}")) //$NON-NLS-1$
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
                .andExpect(jsonPath("$.additionalData[0].key").value("DMS_DB_USER")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.personalIdentificationNumber").value("12934449907")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.hostUrl").value("pantheon.sm-it.hr")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.port").value(1666)) //$NON-NLS-1$
                .andExpect(jsonPath("$.uuid").value(MANDATOR_UUID.toString())) //$NON-NLS-1$
                .andExpect(jsonPath("$.customer").doesNotExist()); //$NON-NLS-1$
    }

    @Test
    void unknownPath_returns404_notServerError() throws Exception
    {
        mockMvc.perform(get("/.git/config")) //$NON-NLS-1$
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND")); //$NON-NLS-1$ //$NON-NLS-2$
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
