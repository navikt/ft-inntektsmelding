package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;

public record LukkForespørselRequest(@Valid OrganisasjonsnummerDto orgnummer,
                                     @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                     LocalDate skjæringstidspunkt) {
}
