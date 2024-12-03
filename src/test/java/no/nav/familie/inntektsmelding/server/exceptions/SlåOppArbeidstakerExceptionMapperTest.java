package no.nav.familie.inntektsmelding.server.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;

import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.FantIkkeArbeidstakerException;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.InnsenderHarIkkeTilgangTilArbeidsforholdException;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.SlåOppArbeidstakerException;

import no.nav.vedtak.log.mdc.MDCOperations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.qos.logback.classic.Level;
import no.nav.vedtak.log.util.MemoryAppender;

@ExtendWith(MockitoExtension.class)
class SlåOppArbeidstakerExceptionMapperTest {

    private static MemoryAppender logSniffer;
    private final SlåOppArbeidstakerExceptionMapper exceptionMapper = new SlåOppArbeidstakerExceptionMapper();

    @BeforeEach
    void setUp() {
        logSniffer = MemoryAppender.sniff(SlåOppArbeidstakerException.class);
        MDCOperations.putCallId();
    }

    @Test
    void skal_mappe_innsender_har_ikke_tilgang_exception() {
        var exception = new InnsenderHarIkkeTilgangTilArbeidsforholdException();
        
        try (var response = exceptionMapper.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            
            var feilDto = (FeilDto) response.getEntity();
            assertThat(feilDto.type()).isEqualTo(FeilType.INNSENDER_HAR_IKKE_TILGANG_TIL_ARBEIDSFORHOLD_FEIL);
            assertThat(feilDto.feilmelding()).isEqualTo("Innsender har ikke tilgang til noen av arbeidsforholdene til arbeidstaker");
            assertThat(feilDto.callId()).isNotNull();
            
            assertThat(logSniffer.search("Uventet feil", Level.ERROR)).isEmpty();
        }
    }

    @Test
    void skal_mappe_fant_ikke_arbeidstaker_exception() {
        var exception = new FantIkkeArbeidstakerException();
        
        try (var response = exceptionMapper.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            
            var feilDto = (FeilDto) response.getEntity();
            assertThat(feilDto.type()).isEqualTo(FeilType.FANT_IKKE_ARBEIDSTAKER_FEIL);
            assertThat(feilDto.feilmelding()).isEqualTo("Fant ikke arbeidstaker");
            assertThat(feilDto.callId()).isNotNull();
            
            assertThat(logSniffer.search("Uventet feil", Level.ERROR)).isEmpty();
        }
    }

    @Test
    void skal_mappe_ukjent_exception() {
        var exception = new SlåOppArbeidstakerException("Test ukjent feil");
        
        try (var response = exceptionMapper.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            
            var feilDto = (FeilDto) response.getEntity();
            assertThat(feilDto.type()).isEqualTo(FeilType.GENERELL_FEIL);
            assertThat(feilDto.feilmelding()).isEqualTo("Uventet feil");
            assertThat(feilDto.callId()).isNotNull();
            
            assertThat(logSniffer.search("Uventet feil", Level.ERROR)).hasSize(1);
        }
    }
}
