package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

/*
 * Dokumentasjon https://confluence.adeo.no/display/FEL/AAREG+-+Tjeneste+REST+aareg.api
 * Swagger https://aareg-services-q2.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v1#/arbeidstaker/finnArbeidsforholdPrArbeidstaker
 * Swagger V2 https://aareg-services-q2.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2#/arbeidstaker/finnArbeidsforholdPrArbeidstaker
 */

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "aareg.rs.url",
    endpointDefault = "http://aareg-services-nais.arbeidsforhold/api/v1/arbeidstaker",
    scopesProperty = "aareg.scopes", scopesDefault = "api://prod-fss.arbeidsforhold.aareg-services-nais/.default")
public class AaregRestKlient {

    private final RestClient restClient; // Setter på consumer-token fra STS
    private final RestConfig restConfig;

    public AaregRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<ArbeidsforholdDto> finnArbeidsforholdForArbeidstaker(String ident, LocalDate qfom, LocalDate qtom) {
        try {
            var target = UriBuilder.fromUri(restConfig.endpoint())
                .path("arbeidsforhold")
                .queryParam("ansettelsesperiodeFom", String.valueOf(qfom))
                .queryParam("ansettelsesperiodeTom", String.valueOf(qtom))
                .queryParam("regelverk", "A_ORDNINGEN")
                .queryParam("historikk", "true")
                .queryParam("sporingsinformasjon", "false")
                .build();
            var request = RestRequest.newGET(target, restConfig).header(NavHeaders.HEADER_NAV_PERSONIDENT, ident);
            var result = restClient.send(request, ArbeidsforholdDto[].class);
            return Arrays.asList(result);
        } catch (UriBuilderException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for finnArbeidsforholdForArbeidstaker");
        }
    }

}
