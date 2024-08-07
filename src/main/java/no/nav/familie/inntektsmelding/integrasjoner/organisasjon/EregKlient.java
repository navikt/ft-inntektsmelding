package no.nav.familie.inntektsmelding.integrasjoner.organisasjon;

import jakarta.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.organisasjon.AbstractOrganisasjonKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;


@Dependent
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "organisasjon.rs.url", endpointDefault = "https://ereg-services.intern.nav.no/api/v2/organisasjon")
class EregKlient extends AbstractOrganisasjonKlient {
    EregKlient() {
        super();
    }
}

