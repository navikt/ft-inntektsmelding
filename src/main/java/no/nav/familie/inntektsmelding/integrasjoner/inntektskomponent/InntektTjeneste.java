package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektIdent;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.Inntekt;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.InntektType;
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeBolkResponse;
import no.nav.vedtak.exception.IntegrasjonException;

@ApplicationScoped
public class InntektTjeneste {
    private static final int DAG_I_MÅNED_RAPPORTERINGSFRIST = 6;
    private InntektskomponentKlient inntektskomponentKlient;

    InntektTjeneste() {
        // CDI
    }

    @Inject
    public InntektTjeneste(InntektskomponentKlient inntektskomponentKlient) {
        this.inntektskomponentKlient = inntektskomponentKlient;
    }

    // Tar inn dagens dato som parameter for å gjøre det enklere å skrive tester
    public List<Månedsinntekt> hentInntekt(AktørIdEntitet aktørId, LocalDate startdato, LocalDate dagensDato, String organisasjonsnummer) {
        var rapporteringsfrist = startdato.withDayOfMonth(DAG_I_MÅNED_RAPPORTERINGSFRIST);
        var erFristPassert = dagensDato.isAfter(rapporteringsfrist);
        var antallMånederTilbakeViBerOm = erFristPassert ? 3 : 4;
        var fomDato = startdato.minusMonths(antallMånederTilbakeViBerOm);
        var tomDato = startdato.minusMonths(1);
        var request = lagRequest(aktørId, fomDato, tomDato);
        var respons = inntektskomponentKlient.finnInntekt(request);
        var inntekter = oversettRespons(respons, aktørId, organisasjonsnummer);
        var alleMåneder = inntekter.size() == antallMånederTilbakeViBerOm
                          ? inntekter
                          : fyllInnManglendeMåneder(fomDato, antallMånederTilbakeViBerOm, organisasjonsnummer, inntekter);
        return justerListeOm4MånederMedInntekt(alleMåneder);
    }

    private static List<Månedsinntekt> justerListeOm4MånederMedInntekt(List<Månedsinntekt> alleMåneder) {
        // Vi fant inntekt på alle måneder vi spurte om, fjerner den eldste
        if (alleMåneder.size() == 4 && alleMåneder.stream().noneMatch(m -> m.beløp == null)) {
            alleMåneder.sort(Comparator.comparing(m -> m.måned));
            return alleMåneder.subList(1, alleMåneder.size());
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
                null,
                organisasjonsnummer))
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

        inntektPerMånedForBruker.forEach(inntektMåned -> inntektMåned.getArbeidsInntektInformasjon()
            .getInntektListe()
            .stream()
            .filter(inntekt -> InntektType.LOENNSINNTEKT.equals(inntekt.getInntektType()) && organisasjonsnummer.equals(
                inntekt.getVirksomhet().getIdentifikator()))
            .findFirst()
            .map(this::mapMånedsInntekt)
            .ifPresent(månedsInntektListe::add));

        return månedsInntektListe;
    }

    private Månedsinntekt mapMånedsInntekt(Inntekt månedsInntekt) {
        return new Månedsinntekt(månedsInntekt.getUtbetaltIMaaned(), månedsInntekt.getBeloep(), månedsInntekt.getVirksomhet().getIdentifikator());
    }

    public record Månedsinntekt(YearMonth måned, BigDecimal beløp, String organisasjonsnummer) {
    }

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
