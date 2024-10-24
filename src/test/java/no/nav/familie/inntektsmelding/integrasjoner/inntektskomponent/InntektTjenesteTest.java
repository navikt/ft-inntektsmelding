package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.tjenester.aordningen.inntektsinformasjon.Aktoer;
import no.nav.tjenester.aordningen.inntektsinformasjon.AktoerType;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektIdent;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektInformasjon;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektMaaned;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.Inntekt;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.InntektType;
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeBolkResponse;

@ExtendWith(MockitoExtension.class)
class InntektTjenesteTest {
    private static final String ORGNR = "111111111";
    private static final String AKTØR_ID = "9999999999999";

    @Mock
    private InntektskomponentKlient klient;
    private InntektTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new InntektTjeneste(klient);
    }

    @Test
    void skal_teste_at_inntekter_innhentes_happy_case() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = stp.plusDays(10);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntekter = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 7), BigDecimal.valueOf(25_000), ORGNR)
            , new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 8), BigDecimal.valueOf(25_000), ORGNR)
            , new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 9), BigDecimal.valueOf(25_000), ORGNR));
        assertThat(inntekter).hasSize(forventetListe.size());
        assertThat(inntekter).containsAll(forventetListe);
    }

    @Test
    void skal_teste_at_inntekter_innhentes_dagens_dato_er_før_rapporteringsfrist_og_inntekt_ikke_finnes_for_siste_måned() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,1);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 6), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,6), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntekter = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 6), BigDecimal.valueOf(25_000), ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 7), BigDecimal.valueOf(25_000), ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 8), BigDecimal.valueOf(25_000), ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 9), null, ORGNR));
        assertThat(inntekter).hasSize(forventetListe.size());
        assertThat(inntekter).containsAll(forventetListe);
    }

    @Test
    void skal_teste_at_inntekter_innhentes_dagens_dato_er_før_rapporteringsfrist_og_inntekt_finnes_for_siste_måned() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,1);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 6), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,6), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        var inntekt4 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3, inntekt4));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntekter = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 7), BigDecimal.valueOf(25_000), ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 8), BigDecimal.valueOf(25_000), ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 9), BigDecimal.valueOf(25_000), ORGNR));
        assertThat(inntekter).hasSize(forventetListe.size());
        assertThat(inntekter).containsAll(forventetListe);
    }

    @Test
    void skal_teste_at_inntekter_innhentes_når_det_mangler_inntekt_midt_i_perioden_uten_justering() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,15);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8), null);
        var inntekt3 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntekter = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 7), BigDecimal.valueOf(25_000), ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 8), null, ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 9), BigDecimal.valueOf(25_000), ORGNR));
        assertThat(inntekter).hasSize(forventetListe.size());
        assertThat(inntekter).containsAll(forventetListe);
    }

    @Test
    void skal_teste_at_inntekter_innhentes_når_det_mangler_inntekt_midt_i_perioden_med_justering() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,2);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 6), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,6), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,7), null);
        var inntekt3 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        var inntekt4 = getInntekt(YearMonth.of(2024,9), null);
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3, inntekt4));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntekter = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 6), BigDecimal.valueOf(25_000), ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 7), null, ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 8), BigDecimal.valueOf(25_000), ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 9), null, ORGNR));
        assertThat(inntekter).hasSize(forventetListe.size());
        assertThat(inntekter).containsAll(forventetListe);
    }

    @Test
    void skal_teste_når_ingen_inntekter_er_rapportert() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,15);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        aiResponse.setArbeidsInntektMaaned(List.of());
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntekter = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 7), null, ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 8), null, ORGNR),
            new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 9), null, ORGNR));
        assertThat(inntekter).hasSize(forventetListe.size());
        assertThat(inntekter).containsAll(forventetListe);
    }

    private static ArbeidsInntektMaaned getInntekt(YearMonth årMåned, BigDecimal beløp) {
        var inntektMånedResponse = new ArbeidsInntektMaaned();
        inntektMånedResponse.setAarMaaned(årMåned);

        var inntekt = new Inntekt();
        inntekt.setVirksomhet(new Aktoer(ORGNR, AktoerType.ORGANISASJON));
        inntekt.setInntektType(InntektType.LOENNSINNTEKT);
        inntekt.setUtbetaltIMaaned(årMåned);
        inntekt.setBeloep(beløp);
        var aiInformasjon = new ArbeidsInntektInformasjon();
        aiInformasjon.setInntektListe(Collections.singletonList(inntekt));
        inntektMånedResponse.setArbeidsInntektInformasjon(aiInformasjon);
        return inntektMånedResponse;
    }

}
