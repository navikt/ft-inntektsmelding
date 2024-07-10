package no.nav.familie.inntektsmelding.server.auth.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import no.nav.vedtak.sikkerhet.oidc.config.ConfigProvider;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;

public class MultiIssuerConfiguration {

    private final Map<String, IssuerConfiguration> issuers = new HashMap<>();

    public MultiIssuerConfiguration(Set<OpenIDProvider> providers) {
        providers.forEach(provider -> {
            var openIDConfiguration = ConfigProvider.getOpenIDConfiguration(provider);
            if (openIDConfiguration.isPresent()) {
                var issuerConfig = openIDConfiguration.get();
                issuers.put(provider.name(), new IssuerConfiguration(provider, issuerConfig));
                issuers.put(issuerConfig.issuer().toString(), new IssuerConfiguration(provider, issuerConfig));
            }
        });
    }

    public Map<String, IssuerConfiguration> getIssuers() {
        return issuers;
    }
}
