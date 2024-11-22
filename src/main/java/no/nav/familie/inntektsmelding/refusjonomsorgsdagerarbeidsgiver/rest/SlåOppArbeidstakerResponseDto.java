package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import java.util.List;

public record SlåOppArbeidstakerResponseDto(String fornavn, String mellomnavn, String etternavn, List<ArbeidsforholdDto> arbeidsforhold) {
}
