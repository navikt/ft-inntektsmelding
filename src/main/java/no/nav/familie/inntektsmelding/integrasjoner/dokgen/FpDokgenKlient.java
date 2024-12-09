package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED,
    endpointProperty = "fpdokgen.url",
    endpointDefault = "http://fpdokgen.teamforeldrepenger",
    application = FpApplication.FPDOKGEN)
public class FpDokgenKlient {
    private final RestClient restClient;
    private final RestConfig restConfig;
    private String templatePath;
    private String templateType;

    @Inject
    public FpDokgenKlient(
        @KonfigVerdi(value = "pdf.template.path", defaultVerdi = "/template/fpinntektsmelding-inntektsmelding/PDFINNTEKTSMELDING") String tempatePath,
        @KonfigVerdi(value = "pdf.template.type", defaultVerdi = "/create-pdf-format-variation") String templateType
    ) {
        this(RestClient.client());
        this.templatePath = tempatePath;
        this.templateType = templateType;
    }

    public FpDokgenKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(FpDokgenKlient.class);
    }

    public byte[] genererPdf(InntektsmeldingPdfData dokumentdata) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + templatePath + templateType);
        var request = RestRequest.newPOSTJson(dokumentdata, endpoint, restConfig);
        var pdf = restClient.sendReturnByteArray(request);

        if (pdf == null || pdf.length == 0) {
            throw new TekniskException("FPIM", "Fikk tomt svar ved kall til dokgen for generering av pdf for inntektsmelding");
        }
        return pdf;
    }
}
