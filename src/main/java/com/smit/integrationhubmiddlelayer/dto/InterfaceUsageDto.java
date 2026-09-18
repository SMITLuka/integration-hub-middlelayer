package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * One Mandator/Company pair that has instantiated an Interface's Mapping or Configuration Template.
 */
@Data
@AllArgsConstructor
public class InterfaceUsageDto
{
    private Long mandatorId;
    private String mandatorName;
    private Long companyId;
    private String companyName;
    private Instant updatedAt;
}
