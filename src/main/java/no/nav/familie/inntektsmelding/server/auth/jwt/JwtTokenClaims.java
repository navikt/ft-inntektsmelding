package no.nav.familie.inntektsmelding.server.auth.jwt;

import java.time.Instant;
import java.util.Map;

import org.jose4j.jwt.JwtClaims;

import no.nav.vedtak.sikkerhet.oidc.validator.JwtUtil;

public class JwtTokenClaims {

    private final String issuer;
    private final Instant expirationTime;
    private final String subject;
    private final Map<String, Object> allClaims;

    public JwtTokenClaims(JwtClaims claims) {
        this.issuer = JwtUtil.getIssuer(claims);
        this.expirationTime = JwtUtil.getExpirationTime(claims);
        this.subject = JwtUtil.getSubject(claims);
        this.allClaims = claims.getClaimsMap();
    }

    public String getIssuer() {
        return issuer;
    }

    public Instant getExpirationTime() {
        return expirationTime;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, Object> getAllClaims() {
        return allClaims;
    }
}
