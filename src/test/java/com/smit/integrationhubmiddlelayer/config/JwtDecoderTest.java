package com.smit.integrationhubmiddlelayer.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the production JwtDecoder bean against real RS256-signed tokens shaped like the ones the
 * SMIT SSO gateway (PyJWT) issues: typ "JWT", kid "sso-gateway-1", no iss/aud, keys fetched from
 * /.well-known/jwks.json over HTTP.
 */
class JwtDecoderTest
{
    private static final String APP_ID = "e9e995e9-4046-43d0-ac03-ee3c563c9f7f"; //$NON-NLS-1$
    private static final String KEY_ID = "sso-gateway-1"; //$NON-NLS-1$

    private static HttpServer jwksServer;
    private static RSAKey signingKey;
    private static JwtDecoder decoder;

    @BeforeAll
    static void startJwksServer() throws Exception
    {
        signingKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        byte[] jwks = new JWKSet(signingKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);

        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); //$NON-NLS-1$
        jwksServer.createContext("/.well-known/jwks.json", exchange -> //$NON-NLS-1$
        {
            exchange.getResponseHeaders().add("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
            exchange.sendResponseHeaders(200, jwks.length);
            exchange.getResponseBody().write(jwks);
            exchange.close();
        });
        jwksServer.start();
        String jwksUri = "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/.well-known/jwks.json"; //$NON-NLS-1$ //$NON-NLS-2$
        decoder = new SecurityConfig().jwtDecoder(jwksUri, APP_ID);
    }

    @AfterAll
    static void stopJwksServer()
    {
        jwksServer.stop(0);
    }

    @Test
    void decode_acceptsSsoTokenForThisApp() throws Exception
    {
        Jwt jwt = decoder.decode(sign(JOSEObjectType.JWT, KEY_ID, signingKey, claims(APP_ID).build()));

        assertThat(jwt.getSubject()).isEqualTo("8d0c3f4e-1111-4222-8333-944455556666"); //$NON-NLS-1$
        assertThat(jwt.getClaimAsString("display_name")).isEqualTo("Luka Lozic"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(new PermissionPolicy().authorities(jwt)).isNotEmpty();
    }

    @Test
    void decode_acceptsTokenWithoutTypHeader() throws Exception
    {
        assertThat(decoder.decode(sign(null, KEY_ID, signingKey, claims(APP_ID).build())).getSubject()).isNotNull();
    }

    @Test
    void decode_rejectsTokenForAnotherSsoApp() throws Exception
    {
        String pulseToken = sign(JOSEObjectType.JWT, KEY_ID, signingKey, claims("d74c5004-8a63-48c9-b54b-3be0b3154b3f").build()); //$NON-NLS-1$

        assertThatThrownBy(() -> decoder.decode(pulseToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsPortalTokenWithoutAppId() throws Exception
    {
        String portalToken = sign(JOSEObjectType.JWT, KEY_ID, signingKey, claims(null).build());

        assertThatThrownBy(() -> decoder.decode(portalToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsTokenWithoutExpiry() throws Exception
    {
        String noExpiry = sign(JOSEObjectType.JWT, KEY_ID, signingKey, claims(APP_ID).expirationTime(null).build());

        assertThatThrownBy(() -> decoder.decode(noExpiry)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsExpiredToken() throws Exception
    {
        Instant past = Instant.now().minusSeconds(7200);
        String expired = sign(JOSEObjectType.JWT, KEY_ID, signingKey,
                claims(APP_ID).issueTime(Date.from(past)).expirationTime(Date.from(past.plusSeconds(3600))).build());

        assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsTokenSignedWithUnknownKey() throws Exception
    {
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();

        assertThatThrownBy(() -> decoder.decode(sign(JOSEObjectType.JWT, KEY_ID, otherKey, claims(APP_ID).build())))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsTokenWithUnknownKeyId() throws Exception
    {
        String unknownKid = sign(JOSEObjectType.JWT, "rotated-key", signingKey, claims(APP_ID).build()); //$NON-NLS-1$

        assertThatThrownBy(() -> decoder.decode(unknownKid)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsHmacTokenEvenWithTheRightKeyId() throws Exception
    {
        // Algorithm confusion: an HS256 token "signed" with key material must never pass as RS256.
        SignedJWT hmac = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(KEY_ID).type(JOSEObjectType.JWT).build(),
                claims(APP_ID).build());
        hmac.sign(new MACSigner(new byte[32]));

        assertThatThrownBy(() -> decoder.decode(hmac.serialize())).isInstanceOf(JwtException.class);
    }

    @Test
    void adminFlagFromARealToken_grantsDelete_onlyAsJsonBoolean() throws Exception
    {
        assertThat(rightsOf(Map.of("is_app_admin", true))).contains("CONFIGURATIONS_MAPPINGS_DELETE"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(rightsOf(Map.of("is_app_admin", "true"))).doesNotContain("CONFIGURATIONS_MAPPINGS_DELETE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /** Signs, decodes and converts a token with these feature flags, so the real JSON parser decides the claim types. */
    private static List<String> rightsOf(Map<String, Object> featureFlags) throws Exception
    {
        JWTClaimsSet claims = claims(APP_ID)
                .claim("permissions", Map.of("feature_flags", featureFlags, "data_scope", Map.of())) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .build();
        Jwt jwt = decoder.decode(sign(JOSEObjectType.JWT, KEY_ID, signingKey, claims));
        return new PermissionPolicy().authorities(jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static String sign(JOSEObjectType type, String keyId, RSAKey key, JWTClaimsSet claims) throws Exception
    {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).type(type).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    /** The claims the SSO gateway's create_access_token writes; app_id is omitted for portal-only tokens. */
    private static JWTClaimsSet.Builder claims(String appId)
    {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject("8d0c3f4e-1111-4222-8333-944455556666") //$NON-NLS-1$
                .claim("email", "luka@example.test") //$NON-NLS-1$ //$NON-NLS-2$
                .claim("display_name", "Luka Lozic") //$NON-NLS-1$ //$NON-NLS-2$
                .claim("is_admin", false) //$NON-NLS-1$
                .claim("permissions", Map.of("feature_flags", Map.of("is_app_user", true), "data_scope", Map.of())) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                .jwtID("3f1c2b9a-0000-4000-8000-000000000001") //$NON-NLS-1$
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)));
        if (appId != null)
        {
            builder.claim("app_id", appId); //$NON-NLS-1$
        }
        return builder;
    }
}
