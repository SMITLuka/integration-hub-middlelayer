package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when attempting to revert an Additional Data override that was never set
 * at the level being addressed (nothing to delete or edit at that level).
 */
public class AdditionalDataKeyNotFoundException extends NotFoundException
{
    public AdditionalDataKeyNotFoundException(String key)
    {
        super("ADDITIONAL_DATA_KEY_NOT_FOUND", "Additional data key not set at this level: " + key); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
