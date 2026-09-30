package com.smit.integrationhubmiddlelayer.controller;

import com.smit.integrationhubmiddlelayer.config.AccessLevel;
import com.smit.integrationhubmiddlelayer.config.AccessRight;
import com.smit.integrationhubmiddlelayer.dto.CurrentUserDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tells the frontend who is logged in and which rights they have. The rights are read from the
 * request's authorities, i.e. exactly what the security rules enforce, so the UI cannot drift from them.
 */
@RestController
public class CurrentUserController
{
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE) //$NON-NLS-1$
    public ResponseEntity<CurrentUserDto> getCurrentUser(Authentication authentication)
    {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        List<CurrentUserDto.LevelPermissions> permissions = Arrays.stream(AccessLevel.values())
                .map(level -> new CurrentUserDto.LevelPermissions(level,
                        authorities.contains(level.authority(AccessRight.READ)),
                        authorities.contains(level.authority(AccessRight.WRITE)),
                        authorities.contains(level.authority(AccessRight.DELETE))))
                .toList();

        String name = null;
        String email = null;
        if (authentication.getPrincipal() instanceof Jwt jwt)
        {
            name = jwt.getClaimAsString("name"); //$NON-NLS-1$
            email = jwt.getClaimAsString("email"); //$NON-NLS-1$
        }
        return ResponseEntity.ok(new CurrentUserDto(name, email, permissions));
    }
}
