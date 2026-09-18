package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when an Interface has no Mapping Template defined.
 */
public class MappingTemplateNotFoundException extends NotFoundException
{
    public MappingTemplateNotFoundException(Long interfaceId)
    {
        super("MAPPING_TEMPLATE_NOT_FOUND", "No mapping template defined for interface: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
