package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InnsendingSoknadDto(
    // Base fields from RefusjonOmsorgspengerArbeidsgiverSkjemaStateSchema
    Kontaktperson kontaktperson,
    String årForRefusjon,
    String harUtbetaltLønn,
    String ansattesFødselsnummer,
    String valgtArbeidsforhold,
    String harDekket10FørsteOmsorgsdager,
    List<FravaerHeleDager> fraværHeleDager,
    List<FravaerDelerAvDag> fraværDelerAvDagen,
    BigDecimal inntekt,
    InntektEndringsArsak inntektEndringsÅrsak,
    Boolean skalRefunderes,
    BigDecimal refusjonsbelopPerMåned,
    Boolean endringIRefusjon,
    List<Refusjonsendring> refusjonsendringer,
    Boolean misterNaturalytelser,
    List<BortfaltNaturalytelsePeriode> bortfaltNaturalytelsePerioder
) {
    public record Kontaktperson(
        String navn,
        String telefonnummer
    ) {}

    public record FravaerHeleDager(
        LocalDate fom,
        LocalDate tom
    ) {}

    public record FravaerDelerAvDag(
        LocalDate dato,
        Double normalArbeidstid,
        Double timerFravaer
    ) {}

    public record InntektEndringsArsak(
        String arsak,
        BigDecimal korrigertInntekt,
        LocalDate fom,
        LocalDate tom
    ) {}

    public record Refusjonsendring(
        LocalDate fom,
        BigDecimal belop
    ) {}

    public record BortfaltNaturalytelsePeriode(
        NaturalytelsesType navn,
        BigDecimal belop,
        LocalDate fom,
        LocalDate tom,
        boolean inkluderTom
    ) {}

    public enum NaturalytelsesType {
        ELEKTRISK_KOMMUNIKASJON,
        AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
        LOSJI,
        KOST_DOEGN,
        BESOKSREISER_HJEMMET_ANNET,
        KOSTBESPARELSE_I_HJEMMET,
        RENTEFORDEL_LAN,
        BIL,
        KOST_DAGER,
        BOLIG,
        SKATTEPLIKTIG_DEL_FORSIKRINGER,
        FRI_TRANSPORT,
        OPSJONER,
        TILSKUDD_BARNEHAGEPLASS,
        ANNET,
        BEDRIFTSBARNEHAGEPLASS,
        YRKEBIL_TJENESTLIGBEHOV_KILOMETER,
        YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS,
        INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING
    }
}
