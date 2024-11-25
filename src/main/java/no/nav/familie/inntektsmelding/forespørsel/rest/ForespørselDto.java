package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

public record ForespørselDto(@NotNull LocalDate skjæringstidspunkt, @NotNull @Valid OrganisasjonsnummerDto orgnr, boolean skalSperresForEndringer) {
}
