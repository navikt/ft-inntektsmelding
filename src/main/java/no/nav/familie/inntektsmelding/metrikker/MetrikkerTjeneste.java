package no.nav.familie.inntektsmelding.metrikker;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;

public class MetrikkerTjeneste {

    // Hvor mange dager er det mellom opprettelse og løsning av oppgaven når inntektsmelding sendes inn via eksternt system
    private static final DistributionSummary  OPPGAVE_VARIGHET_EKSTERN_TELLER =  Metrics.summary("ftinntektsmelding.oppgaver.varighet.ekstern");

    // Hvor mange dager er det mellom opprettelse og løsning av oppgaven når inntektsmelding sendes inn via vårt eget skjema
    private static final DistributionSummary  OPPGAVE_VARIGHET_INTERN_TELLER =  Metrics.summary("ftinntektsmelding.oppgaver.varighet.intern");

    // Måler opprettelse av oppgaver per ytelse
    private static final String COUNTER_FORESPØRRSEL = "ftinntektsmelding.oppgaver.opprettet";

    // Måler mottak av inntektsmeldinger i ftinntektsmelding per ytelse
    private static final String COUNTER_INNTEKTSMELDING = "ftinntektsmelding.inntektsmeldinger.mottatt";

    // Måler årsaker til endring av inntekt i inntektsmeldinger innsendt i ftinntektsmelding
    private static final Map<Ytelsetype, String> COUNTER_YTELLSE_METRIC_ÅRSAK_MAP = Map.of(Ytelsetype.FORELDREPENGER, "ftinntektsmelding.inntektsmeldinger.fp.endringsaarsak",
        Ytelsetype.SVANGERSKAPSPENGER, "ftinntektsmelding.inntektsmeldinger.svp.endringsaarsak",
        Ytelsetype.OMSORGSPENGER, "ftinntektsmelding.inntektsmeldinger.omp.endringsaarsak",
        Ytelsetype.PLEIEPENGER_SYKT_BARN, "ftinntektsmelding.inntektsmeldinger.psb.endringsaarsak",
        Ytelsetype.PLEIEPENGER_NÆRSTÅENDE, "ftinntektsmelding.inntektsmeldinger.ppn.endringsaarsak");

    // Måler hvor mange oppgaver som lukkes etter at fagsystem melder om en inntektsmelding ftinntektsmelding ikke kjenner til
    private static final String COUNTER_LUKK_EKSTERN = "ftinntektsmelding.oppgaver.lukk.ekstern";

    public static void loggForespørselOpprettet(Ytelsetype ytelsetype) {
        var tags = new ArrayList<Tag>();
        tags.add(new ImmutableTag("ytelse", ytelsetype.name()));
        Metrics.counter(COUNTER_FORESPØRRSEL, tags).increment();
    }

    public static void loggForespørselLukkEkstern(ForespørselEntitet forespørsel) {
        var tags = new ArrayList<Tag>();
        tags.add(new ImmutableTag("ytelse", forespørsel.getYtelseType().name()));
        Metrics.counter(COUNTER_LUKK_EKSTERN, tags).increment();
        var dagerSidenOpprettelse = finnAntallDagerÅpen(forespørsel);
        OPPGAVE_VARIGHET_EKSTERN_TELLER.record(dagerSidenOpprettelse);
    }

    private static long finnAntallDagerÅpen(ForespørselEntitet forespørsel) {
        var opprettetDato = forespørsel.getOpprettetTidspunkt().toLocalDate();
        var dagerSidenOpprettelse = ChronoUnit.DAYS.between(opprettetDato, LocalDate.now());
        return dagerSidenOpprettelse;
    }

    public static void loggInnsendtInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        var tags = new ArrayList<Tag>();
        var harOppgittRefusjon = inntektsmelding.getMånedRefusjon() != null && inntektsmelding.getMånedRefusjon().compareTo(BigDecimal.ZERO) > 0;
        var harOppgittEndringerIRefusjon = inntektsmelding.getRefusjonsendringer() != null && !inntektsmelding.getRefusjonsendringer().isEmpty();
        var harOppgittOpphørAvRefusjon = inntektsmelding.getOpphørsdatoRefusjon() != null && !inntektsmelding.getOpphørsdatoRefusjon().equals(Tid.TIDENES_ENDE);
        var harOppgittNaturalytelse = inntektsmelding.getBorfalteNaturalYtelser() != null && !inntektsmelding.getBorfalteNaturalYtelser().isEmpty();
        var harEndretInntekt = !inntektsmelding.getEndringsårsaker().isEmpty();

        tags.add(new ImmutableTag("ytelse", inntektsmelding.getYtelsetype().name()));
        tags.add(new ImmutableTag("har_endret_inntekt", harEndretInntekt ? "Ja" : "Nei"));
        tags.add(new ImmutableTag("har_oppgitt_refusjon", harOppgittRefusjon ? "Ja" : "Nei"));
        tags.add(new ImmutableTag("har_oppgitt_endring_i_refusjon", harOppgittEndringerIRefusjon ? "Ja" : "Nei"));
        tags.add(new ImmutableTag("har_oppgitt_opphoer_av_refusjon", harOppgittOpphørAvRefusjon ? "Ja" : "Nei"));
        tags.add(new ImmutableTag("har_oppgitt_naturalytelse", harOppgittNaturalytelse ? "Ja" : "Nei"));
        Metrics.counter(COUNTER_INNTEKTSMELDING, tags).increment();

        if (!inntektsmelding.getEndringsårsaker().isEmpty()) {
            var tagsÅrsaker = new ArrayList<Tag>();
            inntektsmelding.getEndringsårsaker().forEach(endring -> tagsÅrsaker.add(new ImmutableTag("aarsak", endring.getÅrsak().name())));
            Metrics.counter(COUNTER_YTELLSE_METRIC_ÅRSAK_MAP.get(inntektsmelding.getYtelsetype()), tagsÅrsaker).increment();
        }
    }

    public static void loggForespørselLukkIntern(ForespørselEntitet forespørsel) {
        var dagerSidenOpprettelse = finnAntallDagerÅpen(forespørsel);
        OPPGAVE_VARIGHET_INTERN_TELLER.record(dagerSidenOpprettelse);
    }
}
