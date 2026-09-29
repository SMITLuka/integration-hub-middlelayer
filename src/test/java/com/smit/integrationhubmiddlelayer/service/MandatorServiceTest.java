package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorUpdateRequest;
import com.smit.integrationhubmiddlelayer.entity.Mandator;
import com.smit.integrationhubmiddlelayer.exception.DuplicateMandatorException;
import com.smit.integrationhubmiddlelayer.repository.MandatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the external Mandator ID rules: blank IDs are stored as NULL (the column is UNIQUE)
 * and an ID already used by another Mandator is rejected on both create and update.
 */
@ExtendWith(MockitoExtension.class)
class MandatorServiceTest
{
    @Mock
    private MandatorRepository mandatorRepository;

    @InjectMocks
    private MandatorService mandatorService;

    @Test
    void create_storesBlankExternalMandatorIdAsNull()
    {
        MandatorCreateRequest request = new MandatorCreateRequest();
        request.setName("Autohaus Longin"); //$NON-NLS-1$
        request.setExternalMandatorId("   "); //$NON-NLS-1$
        when(mandatorRepository.save(any(Mandator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mandatorService.create(request);

        ArgumentCaptor<Mandator> captor = ArgumentCaptor.forClass(Mandator.class);
        verify(mandatorRepository).save(captor.capture());
        assertThat(captor.getValue().getExternalMandatorId()).isNull();
        assertThat(captor.getValue().getUuid()).isNotNull();
        verify(mandatorRepository, never()).findByExternalMandatorId(anyString());
    }

    @Test
    void create_throwsDuplicate_whenExternalMandatorIdTaken()
    {
        MandatorCreateRequest request = new MandatorCreateRequest();
        request.setName("Autohaus Longin"); //$NON-NLS-1$
        request.setExternalMandatorId("B0"); //$NON-NLS-1$
        when(mandatorRepository.findByExternalMandatorId("B0")).thenReturn(Optional.of(mandator(2L, "B0"))); //$NON-NLS-1$ //$NON-NLS-2$

        assertThatThrownBy(() -> mandatorService.create(request)).isInstanceOf(DuplicateMandatorException.class);
        verify(mandatorRepository, never()).save(any());
    }

    @Test
    void update_throwsDuplicate_whenExternalMandatorIdBelongsToAnotherMandator()
    {
        Mandator current = mandator(1L, "B0"); //$NON-NLS-1$
        when(mandatorRepository.findById(1L)).thenReturn(Optional.of(current));
        when(mandatorRepository.findByExternalMandatorId("B1")).thenReturn(Optional.of(mandator(2L, "B1"))); //$NON-NLS-1$ //$NON-NLS-2$

        assertThatThrownBy(() -> mandatorService.update(1L, updateRequest("B1"))) //$NON-NLS-1$
                .isInstanceOf(DuplicateMandatorException.class);
        assertThat(current.getExternalMandatorId()).isEqualTo("B0"); //$NON-NLS-1$
    }

    @Test
    void update_allowsKeepingOwnExternalMandatorId()
    {
        Mandator current = mandator(1L, "B0"); //$NON-NLS-1$
        when(mandatorRepository.findById(1L)).thenReturn(Optional.of(current));
        when(mandatorRepository.findByExternalMandatorId("B0")).thenReturn(Optional.of(current)); //$NON-NLS-1$

        mandatorService.update(1L, updateRequest("B0")); //$NON-NLS-1$

        assertThat(current.getExternalMandatorId()).isEqualTo("B0"); //$NON-NLS-1$
    }

    @Test
    void update_storesBlankExternalMandatorIdAsNull()
    {
        Mandator current = mandator(1L, "B0"); //$NON-NLS-1$
        when(mandatorRepository.findById(1L)).thenReturn(Optional.of(current));

        mandatorService.update(1L, updateRequest("")); //$NON-NLS-1$

        assertThat(current.getExternalMandatorId()).isNull();
    }

    @Test
    void newMandators_getDistinctUuids()
    {
        assertThat(new Mandator().getUuid()).isNotNull();
        assertThat(Mandator.builder().build().getUuid()).isNotEqualTo(Mandator.builder().build().getUuid());
    }

    private static Mandator mandator(Long id, String externalMandatorId)
    {
        return Mandator.builder().id(id).name("Mandator " + id).externalMandatorId(externalMandatorId).build(); //$NON-NLS-1$
    }

    private static MandatorUpdateRequest updateRequest(String externalMandatorId)
    {
        MandatorUpdateRequest request = new MandatorUpdateRequest();
        request.setName("Autohaus Longin"); //$NON-NLS-1$
        request.setExternalMandatorId(externalMandatorId);
        return request;
    }
}
