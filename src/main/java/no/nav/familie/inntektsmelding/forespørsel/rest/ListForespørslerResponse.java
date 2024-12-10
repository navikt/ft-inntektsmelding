package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.util.List;

public record ListForespørslerResponse(
    List<InntektsmeldingForespørselDto> inntektmeldingForespørsler) {
}
