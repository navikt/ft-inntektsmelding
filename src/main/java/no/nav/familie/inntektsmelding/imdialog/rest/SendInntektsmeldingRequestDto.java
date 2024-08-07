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

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record SendInntektsmeldingRequestDto(@NotNull @Valid UUID foresporselUuid, @NotNull @Valid AktørIdDto aktorId,
                                            @NotNull @Valid YtelseTypeDto ytelse, @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                            @NotNull @Valid KontaktpersonDto kontaktperson, @NotNull LocalDate startdato,
                                            @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                            @NotNull List<@Valid RefusjonsperiodeRequestDto> refusjonsperioder,
                                            @NotNull List<@Valid NaturalytelseRequestDto> bortfaltNaturaltytelsePerioder) {
    public record RefusjonsperiodeRequestDto(@NotNull LocalDate fom, LocalDate tom,
                                             @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record NaturalytelseRequestDto(@NotNull LocalDate fom, LocalDate tom, @NotNull NaturalytelsetypeDto naturalytelsetype,
                                          @NotNull Boolean erBortfalt,
                                          @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record KontaktpersonDto(@NotNull String navn, @NotNull String telefonnummer) {
    }

}

