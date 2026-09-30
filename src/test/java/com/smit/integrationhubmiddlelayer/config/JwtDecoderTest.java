package com.smit.integrationhubmiddlelayer.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the production JwtDecoder bean against real RS256-signed tokens shaped like the ones the
 * Bitrix MCP server (oidc-provider) issues: typ "at+jwt", keys fetched from {issuer}/jwks over HTTP.
 */
class JwtDecoderTest
{
    private static final String AUDIENCE = "https://integration-hub.example.test"; //$NON-NLS-1$

    private static HttpServer jwksServer;
    private static RSAKey signingKey;
    private static String issuer;
    private static JwtDecoder decoder;

    @BeforeAll
    static void startJwksServer() throws Exception
    {
        signingKey = new RSAKeyGenerator(2048).keyID("test-key").generate(); //$NON-NLS-1$
        byte[] jwks = new JWKSet(signingKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);

        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); //$NON-NLS-1$
        jwksServer.createContext("/jwks", exchange -> //$NON-NLS-1$
        {
            exchange.getResponseHeaders().add("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
            exchange.sendResponseHeaders(200, jwks.length);
            exchange.getResponseBody().write(jwks);
            exchange.close();
        });
        jwksServer.start();
        issuer = "http://127.0.0.1:" + jwksServer.getAddress().getPort(); //$NON-NLS-1$
        decoder = new SecurityConfig().jwtDecoder(issuer, AUDIENCE, "integration-hub"); //$NON-NLS-1$
    }

    @AfterAll
    static void stopJwksServer()
    {
        jwksServer.stop(0);
    }

    @Test
    void decode_acceptsAtJwtAccessTokenForThisApi() throws Exception
    {
        Jwt jwt = decoder.decode(sign(new JOSEObjectType("at+jwt"), issuer, AUDIENCE)); //$NON-NLS-1$

        assertThat(jwt.getSubject()).isEqualTo("42"); //$NON-NLS-1$
        assertThat(jwt.getClaimAsString("bitrix_user_type")).isEqualTo("employee"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(PermissionPolicy.isIntranetEmployee(jwt)).isTrue();
    }

    @Test
    void decode_rejectsTokenForAnotherResource() throws Exception
    {
        String mcpToken = sign(new JOSEObjectType("at+jwt"), issuer, "https://mcp.sm-it.hr/mcp"); //$NON-NLS-1$ //$NON-NLS-2$

        assertThatThrownBy(() -> decoder.decode(mcpToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsTokenFromAnotherIssuer() throws Exception
    {
        String foreignToken = sign(new JOSEObjectType("at+jwt"), "https://evil.example.test", AUDIENCE); //$NON-NLS-1$ //$NON-NLS-2$

        assertThatThrownBy(() -> decoder.decode(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsJwtThatIsNotAnAccessToken() throws Exception
    {
        // ID tokens from the same provider are typed "JWT" (or untyped) and signed with the same key.
        assertThatThrownBy(() -> decoder.decode(sign(JOSEObjectType.JWT, issuer, AUDIENCE))).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(sign(null, issuer, AUDIENCE))).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsTokenWithoutExpiry() throws Exception
    {
        JWTClaimsSet noExpiry = new JWTClaimsSet.Builder(claims(issuer, AUDIENCE)).expirationTime(null).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID())
                .type(new JOSEObjectType("at+jwt")).build(), noExpiry); //$NON-NLS-1$
        jwt.sign(new RSASSASigner(signingKey));

        assertThatThrownBy(() -> decoder.decode(jwt.serialize())).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsTokenSignedWithUnknownKey() throws Exception
    {
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID("test-key").generate(); //$NON-NLS-1$
        SignedJWT forged = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key") //$NON-NLS-1$
                .type(new JOSEObjectType("at+jwt")).build(), claims(issuer, AUDIENCE)); //$NON-NLS-1$
        forged.sign(new RSASSASigner(otherKey));

        assertThatThrownBy(() -> decoder.decode(forged.serialize())).isInstanceOf(JwtException.class);
    }

    private static String sign(JOSEObjectType type, String tokenIssuer, String audience) throws Exception
    {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).type(type).build(),
                claims(tokenIssuer, audience));
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private static JWTClaimsSet claims(String tokenIssuer, String audience)
    {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(tokenIssuer)
                .audience(List.of(audience))
                .subject("42") //$NON-NLS-1$
                .claim("client_id", "integration-hub") //$NON-NLS-1$ //$NON-NLS-2$
                .claim("bitrix_user_type", "employee") //$NON-NLS-1$ //$NON-NLS-2$
                .claim("bitrix_departments", List.of(1)) //$NON-NLS-1$
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .build();
    }
}
