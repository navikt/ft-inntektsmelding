package no.nav.familie.inntektsmelding.server.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.FantIkkeArbeidstakerException;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.InnsenderHarIkkeTilgangTilArbeidsforholdException;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.SlåOppArbeidstakerException;

import no.nav.vedtak.log.mdc.MDCOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlåOppArbeidstakerExceptionMapper implements ExceptionMapper<SlåOppArbeidstakerException> {

    private static final Logger LOG = LoggerFactory.getLogger(SlåOppArbeidstakerException.class);

    @Override
    public Response toResponse(SlåOppArbeidstakerException e) {
        switch (e) {
            case InnsenderHarIkkeTilgangTilArbeidsforholdException e_ -> {
                return harIkkeTilgangTilArbeidsforhold();
            }
            case FantIkkeArbeidstakerException e_ -> {
                return fantIkkeArbeidstaker();
            }
            default -> {
                LOG.error("Uventet feil", e);
                return Response.serverError()
                    .entity(new FeilDto(FeilType.GENERELL_FEIL, "Uventet feil", MDCOperations.getCallId()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
            }
        }
    }

    private static Response harIkkeTilgangTilArbeidsforhold() {
        return Response.status(Response.Status.OK)
            .entity(new FeilDto(FeilType.INNSENDER_HAR_IKKE_TILGANG_TIL_ARBEIDSFORHOLD_FEIL, "Innsender har ikke tilgang til noen av arbeidsforholdene til arbeidstaker", MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Response fantIkkeArbeidstaker() {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new FeilDto(FeilType.FANT_IKKE_ARBEIDSTAKER_FEIL, "Fant ikke arbeidstaker", MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
