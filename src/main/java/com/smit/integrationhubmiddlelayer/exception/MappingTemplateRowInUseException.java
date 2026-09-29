package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when saving a Mapping Template would remove a row that a Company Mapping still has a value for.
 */
public class MappingTemplateRowInUseException extends ConflictException
{
    public MappingTemplateRowInUseException(String descriptor)
    {
        super("MAPPING_TEMPLATE_ROW_IN_USE", //$NON-NLS-1$
                "Mapping row '" + descriptor + "' cannot be removed: it still has values in one or more company mappings"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
