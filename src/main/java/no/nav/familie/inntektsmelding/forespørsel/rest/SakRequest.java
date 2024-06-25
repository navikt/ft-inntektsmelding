package no.nav.familie.inntektsmelding.forespørsel.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;


public record SakRequest(@NotNull @Valid AktørIdDto aktørId, @NotNull @Valid OrganisasjonsnummerDto orgnummer, @NotNull YtelseTypeDto ytelsetype,
                         @NotNull @Valid SaksnummerDto saksnummer) {
}
