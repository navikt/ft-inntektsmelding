package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.dto;

import jakarta.validation.constraints.NotNull;

public record InnsenderDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn, String telefon) {}
