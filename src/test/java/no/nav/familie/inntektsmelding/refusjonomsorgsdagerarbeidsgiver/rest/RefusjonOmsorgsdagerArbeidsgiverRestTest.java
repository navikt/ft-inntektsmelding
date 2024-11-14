package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;

import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester.OpplysningerTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester.ArbeidstakerTjeneste;

@ExtendWith(MockitoExtension.class)
class RefusjonOmsorgsdagerArbeidsgiverRestTest {
    @Mock
    private ArbeidstakerTjeneste arbeidstakerTjenesteMock;

    @Mock
    private OpplysningerTjeneste opplysningerTjenesteMock;

    private RefusjonOmsorgsdagerArbeidsgiverRest rest;

    @BeforeEach
    void set_up() {
        rest = new RefusjonOmsorgsdagerArbeidsgiverRest(arbeidstakerTjenesteMock, opplysningerTjenesteMock);
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_ok_response_når_arbeidstaker_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var dto = new SlåOppArbeidstakerDto(fnr);
        var arbeidstakerInfo = new SlåOppArbeidstakerResponseDto("fornavn", "mellomnavn", "etternavn", null);

        when(arbeidstakerTjenesteMock.slåOppArbeidstaker(fnr)).thenReturn(arbeidstakerInfo);

        Response response = rest.slåOppArbeidstaker(dto);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(arbeidstakerInfo, response.getEntity());
        verify(arbeidstakerTjenesteMock).slåOppArbeidstaker(fnr);
    }

    @Test
    void slå_opp_arbeidstaker_skal_kaste_not_found_exception_når_arbeidstaker_ikke_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var dto = new SlåOppArbeidstakerDto(fnr);

        when(arbeidstakerTjenesteMock.slåOppArbeidstaker(fnr)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> rest.slåOppArbeidstaker(dto));
        verify(arbeidstakerTjenesteMock).slåOppArbeidstaker(fnr);
    }

    @Test
    void hentOpplysninger_returnerer_som_forventet() {
        var dto = new OpplysningerResponseDto(new InnsenderDto("fornavn", "mellomnavn", "etternavn", "telefon"));

        when(opplysningerTjenesteMock.hentOpplysninger()).thenReturn(dto);

        var response = rest.hentOpplysninger();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(dto, response.getEntity());
        verify(opplysningerTjenesteMock).hentOpplysninger();
    }
}
