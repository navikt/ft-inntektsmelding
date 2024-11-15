package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektIdent;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.Inntekt;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.InntektType;
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeBolkResponse;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class InntektTjeneste {
    private static final int DAG_I_MÅNED_RAPPORTERINGSFRIST = 5;
    private InntektskomponentKlient inntektskomponentKlient;

    InntektTjeneste() {
        // CDI
    }

    @Inject
    public InntektTjeneste(InntektskomponentKlient inntektskomponentKlient) {
        this.inntektskomponentKlient = inntektskomponentKlient;
    }

    // Tar inn dagens dato som parameter for å gjøre det enklere å skrive tester
    public Inntektsopplysninger hentInntekt(AktørIdEntitet aktørId, LocalDate startdato, LocalDate dagensDato, String organisasjonsnummer) {
        var antallMånederViBerOm = finnAntallMånederViMåBeOm(startdato, dagensDato);
        var fomDato = startdato.minusMonths(antallMånederViBerOm);
        var tomDato = startdato.minusMonths(1);
        var request = lagRequest(aktørId, fomDato, tomDato);
        var respons = inntektskomponentKlient.finnInntekt(request);
        var inntekter = oversettRespons(respons, aktørId, organisasjonsnummer);
        var alleMåneder = inntekter.size() == antallMånederViBerOm
                          ? inntekter
                          : fyllInnManglendeMåneder(fomDato, antallMånederViBerOm, organisasjonsnummer, inntekter);
        var kuttetNedTilTreMndInntekt = fjernOverflødigeMånederOmNødvendig(alleMåneder);
        return beregnSnittOgLeggPåStatus(kuttetNedTilTreMndInntekt, dagensDato, organisasjonsnummer);
    }

    private Inntektsopplysninger beregnSnittOgLeggPåStatus(List<Månedsinntekt> inntekter, LocalDate dagensDato, String organisasjonsnummer) {
        var månedsinntekter = inntekter.stream().map(i -> mapInntektMedStatus(i, dagensDato)).toList();
        var antallMndMedRapportertInntekt = månedsinntekter.stream().filter(m -> m.beløp() != null).count();
        if (antallMndMedRapportertInntekt > 3) {
            throw new TekniskException("FPINNTEKTSMELDING_INNTEKTKSKOMPONENT_1",
                String.format("Har mappet flere enn 3 måneder med inntekt, ugyldig tilstand. Mappede månedsinntekter var %s", månedsinntekter));
        }
        var totalLønn = månedsinntekter.stream()
            .filter(m -> m.beløp() != null)
            .map(Inntektsopplysninger.InntektMåned::beløp)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
        var snittlønn = totalLønn.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_EVEN);
        return new Inntektsopplysninger(snittlønn, organisasjonsnummer, månedsinntekter);
    }

    private Inntektsopplysninger.InntektMåned mapInntektMedStatus(Månedsinntekt i, LocalDate dagensDato) {
        var skalInntektVæreRapportert = rapporteringsfristErPassert(i.måned.atDay(1), dagensDato);
        var erInntektRapportert = i.beløp != null;
        if (erInntektRapportert) {
            return new Inntektsopplysninger.InntektMåned(i.beløp, i.måned, MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        }
        return skalInntektVæreRapportert
               ? new Inntektsopplysninger.InntektMåned(i.beløp, i.måned, MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT)
               : new Inntektsopplysninger.InntektMåned(i.beløp, i.måned, MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT);
    }

    private int finnAntallMånederViMåBeOm(LocalDate startdato, LocalDate dagensDato) {
        var beregningsperiodeAntallMnd = 3;
        if (!rapporteringsfristErPassert(startdato.minusMonths(1), dagensDato)) {
            beregningsperiodeAntallMnd++;
        }
        if (!rapporteringsfristErPassert(startdato.minusMonths(2), dagensDato)) {
            beregningsperiodeAntallMnd++;
        }
        return beregningsperiodeAntallMnd;
    }

    private boolean rapporteringsfristErPassert(LocalDate dato, LocalDate dagensDato) {
        return dagensDato.isAfter(dato.plusMonths(1).withDayOfMonth(DAG_I_MÅNED_RAPPORTERINGSFRIST));
    }

    private static List<Månedsinntekt> fjernOverflødigeMånederOmNødvendig(List<Månedsinntekt> alleMåneder) {
        var antallMndMedSattInntekt = alleMåneder.stream().filter(m -> m.beløp != null).toList().size();
        int overflødigeMåneder = antallMndMedSattInntekt > 3 ? antallMndMedSattInntekt-3 : 0;
        // Vi fant inntekt på flere måneder enn vi trenger, fjerner de eldste som er overflødige
        if (overflødigeMåneder > 0) {
            alleMåneder.sort(Comparator.comparing(m -> m.måned));
            return alleMåneder.subList(overflødigeMåneder, alleMåneder.size());
        }
        return alleMåneder;
    }

    public static List<Månedsinntekt> fyllInnManglendeMåneder(LocalDate fomDato,
                                                              long månederViBerOm,
                                                              String organisasjonsnummer,
                                                              List<Månedsinntekt> inntekter) {
        List<Månedsinntekt> liste = new ArrayList<>(inntekter);
        lagTommeInntekter(fomDato, månederViBerOm, organisasjonsnummer).stream()
            .filter(tomInntekt -> inntekter.stream().noneMatch(i -> i.måned.equals(tomInntekt.måned)))
            .forEach(liste::add);
        return liste;
    }

    public static List<Månedsinntekt> lagTommeInntekter(LocalDate fomDato, long månederViBerOm, String organisasjonsnummer) {
        return Stream.iterate(fomDato.withDayOfMonth(1),
                date -> date.plusMonths(1))
            .limit(månederViBerOm)
            .map(fom -> new Månedsinntekt(
                YearMonth.of(fom.getYear(), fom.getMonth()),
                null))
            .collect(Collectors.toList());
    }

    private List<Månedsinntekt> oversettRespons(HentInntektListeBolkResponse response, AktørIdEntitet aktørId, String organisasjonsnummer) {
        if (response.getSikkerhetsavvikListe() != null && !response.getSikkerhetsavvikListe().isEmpty()) {
            throw new IntegrasjonException("FP-535194",
                String.format("Fikk følgende sikkerhetsavvik ved kall til inntektstjenesten: %s.", byggSikkerhetsavvikString(response)));
        }

        List<ArbeidsInntektIdent> inntektListeRespons =
            response.getArbeidsInntektIdentListe() == null ? Collections.emptyList() : response.getArbeidsInntektIdentListe();
        var inntektPerMånedForBruker = inntektListeRespons.stream()
            .filter(a -> a.getIdent().getIdentifikator().equals(aktørId.getAktørId()))
            .findFirst()
            .map(ArbeidsInntektIdent::getArbeidsInntektMaaned)
            .orElse(List.of());

        List<Månedsinntekt> månedsInntektListe = new ArrayList<>();

        inntektPerMånedForBruker.forEach(inntektMåned -> {
            if (inntektMåned.getArbeidsInntektInformasjon() != null && inntektMåned.getArbeidsInntektInformasjon().getInntektListe() != null) {
                var inntekterPrMåned = inntektMåned.getArbeidsInntektInformasjon()
                    .getInntektListe()
                    .stream()
                    .filter(inntekt -> InntektType.LOENNSINNTEKT.equals(inntekt.getInntektType())
                        && organisasjonsnummer.equals(inntekt.getVirksomhet().getIdentifikator()))
                    .collect(Collectors.groupingBy(Inntekt::getUtbetaltIMaaned));
                inntekterPrMåned.forEach((key, value) -> {
                    var totalInntektForMnd = value.stream().map(Inntekt::getBeloep)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
                    var inntekt = new Månedsinntekt(key, totalInntektForMnd);
                    månedsInntektListe.add(inntekt);
                });
            }
        });
        return månedsInntektListe;
    }

    private record Månedsinntekt(YearMonth måned, BigDecimal beløp) {}

    private FinnInntektRequest lagRequest(AktørIdEntitet aktørId, LocalDate fomDato, LocalDate tomDato) {
        var fomÅrMåned = YearMonth.of(fomDato.getYear(), fomDato.getMonth());
        var tomÅrMåned = YearMonth.of(tomDato.getYear(), tomDato.getMonth());

        return new FinnInntektRequest(aktørId.getAktørId(), fomÅrMåned, tomÅrMåned);
    }

    private String byggSikkerhetsavvikString(HentInntektListeBolkResponse response) {
        var stringBuilder = new StringBuilder();
        var sikkerhetsavvikListe = response.getSikkerhetsavvikListe();
        if (sikkerhetsavvikListe != null && !sikkerhetsavvikListe.isEmpty()) {
            stringBuilder.append(sikkerhetsavvikListe.getFirst().getTekst());
            for (int i = 1; i < sikkerhetsavvikListe.size(); i++) {
                stringBuilder.append(", ");
                stringBuilder.append(sikkerhetsavvikListe.get(i).getTekst());
            }
        }
        return stringBuilder.toString();
    }

}
