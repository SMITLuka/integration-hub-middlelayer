package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceDetailDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceUsagesDto;
import com.smit.integrationhubmiddlelayer.exception.DuplicateInterfaceException;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.exception.InterfaceNotFoundException;
import com.smit.integrationhubmiddlelayer.service.InterfaceService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterfaceController.class)
@Import(GlobalExceptionHandler.class)
class InterfaceControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterfaceService interfaceService;

    private static InterfaceDetailDto detail(Long id)
    {
        return new InterfaceDetailDto(id, "Volvo Grip Api", "https://dms", "https://oem", "https://mid", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                List.of(new AdditionalDataEntryDto("Market", "AT")), true, true, //$NON-NLS-1$ //$NON-NLS-2$
                new InterfaceUsagesDto(List.of(), List.of()));
    }

    @Test
    void list_returns200WithPage() throws Exception
    {
        InterfaceSummaryDto summary = new InterfaceSummaryDto(1L, "Volvo Grip Api", true, true); //$NON-NLS-1$
        when(interfaceService.list(isNull(), any())).thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/interfaces")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Volvo Grip Api")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns200WithBody() throws Exception
    {
        when(interfaceService.create(any())).thenReturn(detail(1L));

        mockMvc.perform(post("/interfaces") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Volvo Grip Api\"}")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Volvo Grip Api")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns400_whenNameBlank() throws Exception
    {
        mockMvc.perform(post("/interfaces") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")) //$NON-NLS-1$
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns409_whenNameDuplicate() throws Exception
    {
        when(interfaceService.create(any())).thenThrow(new DuplicateInterfaceException("Volvo Grip Api")); //$NON-NLS-1$

        mockMvc.perform(post("/interfaces") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Volvo Grip Api\"}")) //$NON-NLS-1$
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_INTERFACE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void getDetail_returns200WithBody_whenFound() throws Exception
    {
        when(interfaceService.getDetail(1L)).thenReturn(detail(1L));

        mockMvc.perform(get("/interfaces/1")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMappingTemplate").value(true)); //$NON-NLS-1$
    }

    @Test
    void getDetail_returns404_whenNotFound() throws Exception
    {
        when(interfaceService.getDetail(99L)).thenThrow(new InterfaceNotFoundException(99L));

        mockMvc.perform(get("/interfaces/99")) //$NON-NLS-1$
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INTERFACE_NOT_FOUND")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform(delete("/interfaces/1")) //$NON-NLS-1$
                .andExpect(status().isNoContent());
    }
}
