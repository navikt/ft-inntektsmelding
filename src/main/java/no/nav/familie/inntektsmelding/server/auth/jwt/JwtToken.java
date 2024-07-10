package no.nav.familie.inntektsmelding.server.auth.jwt;

import no.nav.vedtak.sikkerhet.oidc.token.TokenString;
import no.nav.vedtak.sikkerhet.oidc.validator.JwtUtil;

public class JwtToken {

    private final String tokenString;
    private final JwtTokenClaims tokenClaims;

    public JwtToken(String tokenString) {
        this.tokenString = tokenString;
        this.tokenClaims = new JwtTokenClaims(JwtUtil.getClaims(tokenString));
    }

    public String getToken() {
        return tokenString;
    }

    public TokenString getTokenString() {
        return new TokenString(tokenString);
    }

    public JwtTokenClaims getTokenClaims() {
        return tokenClaims;
    }

    public String getIssuer() {
        return tokenClaims.getIssuer();
    }
}
