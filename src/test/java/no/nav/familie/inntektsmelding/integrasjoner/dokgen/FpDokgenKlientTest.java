package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class FpDokgenKlientTest {

    FpDokgenKlient fpDokgenKlient = new FpDokgenKlient("/path", "/path");
    RestClient restClient = mock(RestClient.class);

    @Test
    public void skal_generere_pdf() throws URISyntaxException {
        when(restClient.sendReturnByteArray(any())).thenReturn("hello".getBytes());
        fpDokgenKlient.setRestClient(restClient);
        fpDokgenKlient.genererPdf(new InntektsmeldingPdfData());
    }
}
