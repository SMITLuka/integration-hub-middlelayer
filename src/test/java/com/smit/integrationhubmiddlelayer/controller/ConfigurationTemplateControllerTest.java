package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateEntryDto;
import com.smit.integrationhubmiddlelayer.entity.ConfigValueType;
import com.smit.integrationhubmiddlelayer.exception.ConfigurationTemplateNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateConfigurationTemplateException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateConfigurationTemplateKeyException;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.service.ConfigurationTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(ConfigurationTemplateController.class)
@Import(GlobalExceptionHandler.class)
class ConfigurationTemplateControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigurationTemplateService configurationTemplateService;

    private static ConfigurationTemplateDto dto()
    {
        ConfigurationTemplateEntryDto entry = new ConfigurationTemplateEntryDto(1L, "Market", ConfigValueType.TEXT, //$NON-NLS-1$
                "AT", null, "Target Market (Country Code)", 1); //$NON-NLS-1$ //$NON-NLS-2$
        return new ConfigurationTemplateDto(1L, List.of(entry));
    }

    @Test
    void get_returns200WithBody() throws Exception
    {
        when(configurationTemplateService.get(1L)).thenReturn(dto());

        mockMvc.perform(get("/interfaces/1/configuration-template")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].key").value("Market")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void get_returns404_whenNotDefined() throws Exception
    {
        when(configurationTemplateService.get(1L)).thenThrow(new ConfigurationTemplateNotFoundException(1L));

        mockMvc.perform(get("/interfaces/1/configuration-template")) //$NON-NLS-1$
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFIGURATION_TEMPLATE_NOT_FOUND")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns200WithBody() throws Exception
    {
        when(configurationTemplateService.create(eq(1L), any())).thenReturn(dto());

        mockMvc.perform(post("/interfaces/1/configuration-template") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"key\":\"Market\",\"type\":\"TEXT\"}]}")) //$NON-NLS-1$
                .andExpect(status().isOk());
    }

    @Test
    void create_returns409_whenAlreadyExists() throws Exception
    {
        when(configurationTemplateService.create(eq(1L), any())).thenThrow(new DuplicateConfigurationTemplateException(1L));

        mockMvc.perform(post("/interfaces/1/configuration-template") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[]}")) //$NON-NLS-1$
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CONFIGURATION_TEMPLATE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns400_whenDuplicateKeyInRequest() throws Exception
    {
        when(configurationTemplateService.create(eq(1L), any())).thenThrow(new DuplicateConfigurationTemplateKeyException("Market")); //$NON-NLS-1$

        mockMvc.perform(post("/interfaces/1/configuration-template") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"key\":\"Market\",\"type\":\"TEXT\"},{\"key\":\"Market\",\"type\":\"TEXT\"}]}")) //$NON-NLS-1$
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CONFIGURATION_TEMPLATE_KEY")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void replace_returns200WithBody() throws Exception
    {
        when(configurationTemplateService.replace(eq(1L), any())).thenReturn(dto());

        mockMvc.perform(put("/interfaces/1/configuration-template") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[]}")) //$NON-NLS-1$
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform(delete("/interfaces/1/configuration-template")) //$NON-NLS-1$
                .andExpect(status().isNoContent());
    }
}
