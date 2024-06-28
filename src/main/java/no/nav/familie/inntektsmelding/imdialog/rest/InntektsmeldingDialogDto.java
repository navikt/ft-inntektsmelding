package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InntektsmeldingDialogDto(@Valid @NotNull PersonInfoResponseDto person,
                                       @Valid @NotNull OrganisasjonInfoResponseDto arbeidsgiver,
                                       @Valid @NotNull InnsenderDto innsender,
                                       @Valid @NotNull List<MånedsinntektResponsDto> inntekter,
                                       @NotNull LocalDate startdatoPermisjon,
                                       @Valid @NotNull YtelseTypeDto ytelse,
                                       @Valid @NotNull UUID forespørselUuid) {

    public record PersonInfoResponseDto(@NotNull String fornavn,
                                        @NotNull String mellomnavn,
                                        @NotNull String etternavn,
                                        @NotNull String fødselsnummer,
                                        @NotNull String aktørId) {
    }

    public record OrganisasjonInfoResponseDto(@NotNull String organisasjonNavn, @NotNull String organisasjonNummer) {
    }

    public record InnsenderDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn,
                               @NotNull String fødselsnummer, String telefon) {
    }

    public record MånedsinntektResponsDto(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull BigDecimal beløp,
                                          String arbeidsgiverIdent) {
    }
}
