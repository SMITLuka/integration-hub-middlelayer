package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when trying to revert a Mapping row that has no override set at this Company Mapping.
 */
public class MappingRowOverrideNotFoundException extends NotFoundException
{
    public MappingRowOverrideNotFoundException(Long companyMappingId, Long templateRowId)
    {
        super("MAPPING_ROW_OVERRIDE_NOT_FOUND", //$NON-NLS-1$
                "No override set for row " + templateRowId + " on company mapping " + companyMappingId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
