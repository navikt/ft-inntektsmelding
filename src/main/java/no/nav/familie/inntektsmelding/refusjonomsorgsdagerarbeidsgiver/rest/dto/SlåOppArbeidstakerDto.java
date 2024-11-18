package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

public record SlåOppArbeidstakerDto(@Valid @NotNull PersonIdent fødselsnummer) {
}

