package com.smit.integrationhubmiddlelayer.dto;

import com.smit.integrationhubmiddlelayer.config.AccessLevel;

import java.util.List;

/**
 * The logged-in user and what they may do, for the frontend's profile box and for hiding actions
 * the user has no right to. The backend still enforces every right on each request.
 */
public record CurrentUserDto(String name, String email, List<LevelPermissions> permissions)
{
    /**
     * Rights on one AccessLevel.
     */
    public record LevelPermissions(AccessLevel level, boolean read, boolean write, boolean delete)
    {
    }
}
