package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SlåOppArbeidstakerDto(@Valid @NotNull String fødselsnummer) {
}

