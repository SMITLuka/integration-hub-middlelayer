package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when attempting to create a Mapping Template for an Interface that already has one.
 */
public class DuplicateMappingTemplateException extends ConflictException
{
    public DuplicateMappingTemplateException(Long interfaceId)
    {
        super("DUPLICATE_MAPPING_TEMPLATE", "Interface already has a mapping template: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
