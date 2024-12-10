package no.nav.familie.inntektsmelding.forvaltning.rest;

import java.util.List;

public record ListForespørslerResponse(
    List<InntektsmeldingForespørselDto> inntektmeldingForespørsler) {
}
