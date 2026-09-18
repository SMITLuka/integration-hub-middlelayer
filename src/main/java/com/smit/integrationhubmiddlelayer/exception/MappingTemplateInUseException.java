package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when trying to delete a Mapping Template that still has Company Mappings instantiated from it.
 */
public class MappingTemplateInUseException extends ConflictException
{
    public MappingTemplateInUseException(Long interfaceId)
    {
        super("MAPPING_TEMPLATE_IN_USE", "Mapping template is still in use by one or more companies: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
