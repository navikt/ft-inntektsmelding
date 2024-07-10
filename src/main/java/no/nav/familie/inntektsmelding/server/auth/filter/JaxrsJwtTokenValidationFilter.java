package no.nav.familie.inntektsmelding.server.auth.filter;


import no.nav.familie.inntektsmelding.server.auth.config.MultiIssuerConfiguration;
import no.nav.familie.inntektsmelding.server.auth.validator.TokenValidationHandler;

public class JaxrsJwtTokenValidationFilter extends JwtTokenValidationFilter {

    public JaxrsJwtTokenValidationFilter(MultiIssuerConfiguration oidcConfig) {
        super(new TokenValidationHandler(oidcConfig));
    }
}
