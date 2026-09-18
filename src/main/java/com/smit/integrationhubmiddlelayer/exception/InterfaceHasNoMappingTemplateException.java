package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when trying to instantiate a Mapping on a Company for an Interface that has no Mapping Template.
 */
public class InterfaceHasNoMappingTemplateException extends BadRequestException
{
    public InterfaceHasNoMappingTemplateException(Long interfaceId)
    {
        super("INTERFACE_HAS_NO_MAPPING_TEMPLATE", "Interface has no mapping template defined: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
