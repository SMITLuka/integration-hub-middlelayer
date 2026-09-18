package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.dto.MappingTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateRowDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateSectionDto;
import com.smit.integrationhubmiddlelayer.exception.DuplicateMappingTemplateException;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.exception.MappingTemplateNotFoundException;
import com.smit.integrationhubmiddlelayer.service.MappingTemplateService;
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

@WebMvcTest(MappingTemplateController.class)
@Import(GlobalExceptionHandler.class)
class MappingTemplateControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MappingTemplateService mappingTemplateService;

    private static MappingTemplateDto dto()
    {
        MappingTemplateRowDto row = new MappingTemplateRowDto(1L, "Market", "AT", 1); //$NON-NLS-1$ //$NON-NLS-2$
        MappingTemplateSectionDto section = new MappingTemplateSectionDto(1L, "General", 1, List.of(row)); //$NON-NLS-1$
        return new MappingTemplateDto(1L, List.of(section));
    }

    @Test
    void get_returns200WithBody() throws Exception
    {
        when(mappingTemplateService.get(1L)).thenReturn(dto());

        mockMvc.perform(get("/interfaces/1/mapping-template")) //$NON-NLS-1$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].name").value("General")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void get_returns404_whenNotDefined() throws Exception
    {
        when(mappingTemplateService.get(1L)).thenThrow(new MappingTemplateNotFoundException(1L));

        mockMvc.perform(get("/interfaces/1/mapping-template")) //$NON-NLS-1$
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MAPPING_TEMPLATE_NOT_FOUND")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void create_returns200WithBody() throws Exception
    {
        when(mappingTemplateService.create(eq(1L), any())).thenReturn(dto());

        mockMvc.perform(post("/interfaces/1/mapping-template") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sections\":[{\"name\":\"General\",\"rows\":[{\"descriptor\":\"Market\"}]}]}")) //$NON-NLS-1$
                .andExpect(status().isOk());
    }

    @Test
    void create_returns409_whenAlreadyExists() throws Exception
    {
        when(mappingTemplateService.create(eq(1L), any())).thenThrow(new DuplicateMappingTemplateException(1L));

        mockMvc.perform(post("/interfaces/1/mapping-template") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sections\":[]}")) //$NON-NLS-1$
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_MAPPING_TEMPLATE")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void replace_returns200WithBody() throws Exception
    {
        when(mappingTemplateService.replace(eq(1L), any())).thenReturn(dto());

        mockMvc.perform(put("/interfaces/1/mapping-template") //$NON-NLS-1$
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sections\":[]}")) //$NON-NLS-1$
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform(delete("/interfaces/1/mapping-template")) //$NON-NLS-1$
                .andExpect(status().isNoContent());
    }
}
