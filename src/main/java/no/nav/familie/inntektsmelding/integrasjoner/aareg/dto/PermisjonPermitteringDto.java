package no.nav.familie.inntektsmelding.integrasjoner.aareg.dto;

import java.math.BigDecimal;

public record PermisjonPermitteringDto(PeriodeDto periode, BigDecimal prosent, String type) {

}
