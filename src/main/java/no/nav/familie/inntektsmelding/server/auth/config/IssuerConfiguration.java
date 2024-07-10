package no.nav.familie.inntektsmelding.server.auth.config;

import no.nav.vedtak.sikkerhet.oidc.config.OpenIDConfiguration;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.validator.OidcTokenValidator;

public class IssuerConfiguration {

    protected static final String AUTHORIZATION = "Authorization";
    private final OpenIDProvider shortname;
    private final String acceptedAudience;
    private final String headerName;
    private final OidcTokenValidator tokenValidator;

    public IssuerConfiguration(OpenIDProvider providerName, OpenIDConfiguration openIDConfiguration) {
        this.shortname = providerName;
        this.acceptedAudience = openIDConfiguration.clientId();
        this.headerName = AUTHORIZATION;
        this.tokenValidator = new OidcTokenValidator(openIDConfiguration);
    }

    public String getShortname() {
        return shortname.name();
    }

    public String getAcceptedAudience() {
        return acceptedAudience;
    }

    public String getHeaderName() {
        return headerName;
    }

    public OidcTokenValidator getTokenValidator() {
        return tokenValidator;
    }
}
