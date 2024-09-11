package no.nav.familie.inntektsmelding.imdialog.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.koder.ÅrsakTilInnsending;
import no.nav.familie.inntektsmelding.server.authz.TilgangsstyringInputTyper;
import no.nav.familie.inntektsmelding.server.authz.api.TilgangsstyringDto;
import no.nav.familie.inntektsmelding.server.authz.api.TilgangsstyringInput;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record SendInntektsmeldingRequestDto(@NotNull @Valid UUID foresporselUuid,
                                            @NotNull @Valid AktørIdDto aktorId,
                                            @NotNull @Valid YtelseTypeDto ytelse,
                                            @NotNull ÅrsakTilInnsending årsakTilInnsending,
                                            @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                            @NotNull @Valid KontaktpersonRequestDto kontaktperson,
                                            @NotNull LocalDate startdato,
                                            @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                            @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal refusjon,
                                            @NotNull List<@Valid RefusjonendringRequestDto> refusjonsendringer,
                                            @NotNull List<@Valid BortfaltNaturalytelseRequestDto> bortfaltNaturalytelsePerioder) implements TilgangsstyringDto {

    public record RefusjonendringRequestDto(@NotNull LocalDate fom,
                                            @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record BortfaltNaturalytelseRequestDto(@NotNull LocalDate fom,
                                          LocalDate tom,
                                          @NotNull NaturalytelsetypeDto naturalytelsetype,
                                                  @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record KontaktpersonRequestDto(@NotNull String navn, @NotNull String telefonnummer) {
    }

    @Override
    public TilgangsstyringInput inputAttributer() {
        return TilgangsstyringInput.opprett().leggTil(TilgangsstyringInputTyper.FORESPORSEL_ID, foresporselUuid());
    }
}

