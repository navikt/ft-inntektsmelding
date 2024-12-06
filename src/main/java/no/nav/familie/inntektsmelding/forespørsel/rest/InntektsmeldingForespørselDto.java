package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.UUID;

public record InntektsmeldingForespørselDto(
    UUID uuid,
    LocalDate skjæringstidspunkt,
    String arbeidsgiverident,
    String aktørid,
    String ytelsetype) {}
