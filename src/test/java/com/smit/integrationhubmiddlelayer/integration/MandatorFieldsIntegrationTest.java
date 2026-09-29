package com.smit.integrationhubmiddlelayer.integration;

import com.smit.integrationhubmiddlelayer.AbstractIntegrationTest;
import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Mandator connection fields added in V7 (Personal Identification Number,
 * Host URL, Port) are persisted and returned on create and update. Runs against the real
 * Flyway-migrated schema, so it also guards the V7 migration against drifting from the entity
 * mapping (Hibernate runs with ddl-auto=validate).
 */
class MandatorFieldsIntegrationTest extends AbstractIntegrationTest
{
    @Test
    void createAndUpdate_persistConnectionFields()
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MandatorCreateRequest createRequest = new MandatorCreateRequest();
        createRequest.setName("Autohaus Longin " + suffix); //$NON-NLS-1$
        createRequest.setSystem("Pantheon"); //$NON-NLS-1$
        createRequest.setPersonalIdentificationNumber("12934449907"); //$NON-NLS-1$
        createRequest.setExternalMandatorId("B0_" + suffix); //$NON-NLS-1$
        createRequest.setHostUrl("pantheon.sm-it.hr"); //$NON-NLS-1$
        createRequest.setPort(1666);

        ResponseEntity<MandatorDetailDto> created = restTemplate.postForEntity("/mandators", createRequest, MandatorDetailDto.class); //$NON-NLS-1$
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long mandatorId = created.getBody().getId();

        MandatorDetailDto fetched = restTemplate.getForObject("/mandators/" + mandatorId, MandatorDetailDto.class); //$NON-NLS-1$
        assertThat(fetched.getPersonalIdentificationNumber()).isEqualTo("12934449907"); //$NON-NLS-1$
        assertThat(fetched.getHostUrl()).isEqualTo("pantheon.sm-it.hr"); //$NON-NLS-1$
        assertThat(fetched.getPort()).isEqualTo(1666);

        MandatorUpdateRequest updateRequest = new MandatorUpdateRequest();
        updateRequest.setName(createRequest.getName());
        updateRequest.setHostUrl("pantheon2.sm-it.hr"); //$NON-NLS-1$
        updateRequest.setPort(1667);

        ResponseEntity<MandatorDetailDto> updated = restTemplate.exchange(
                "/mandators/" + mandatorId, HttpMethod.PUT, new HttpEntity<>(updateRequest), MandatorDetailDto.class); //$NON-NLS-1$
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().getHostUrl()).isEqualTo("pantheon2.sm-it.hr"); //$NON-NLS-1$
        assertThat(updated.getBody().getPort()).isEqualTo(1667);
        assertThat(updated.getBody().getPersonalIdentificationNumber()).isNull();
    }
}
