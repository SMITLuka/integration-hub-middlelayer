package com.smit.integrationhubmiddlelayer.config;

/**
 * What a user may do on an AccessLevel: GET is READ, POST/PUT is WRITE, DELETE of a whole entity is DELETE.
 */
public enum AccessRight
{
    READ,
    WRITE,
    DELETE
}
