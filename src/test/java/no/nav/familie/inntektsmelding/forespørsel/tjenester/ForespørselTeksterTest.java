package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForespørselTeksterTest {

    @Test
    void lagSaksTittel() {
        String saksTittel = ForespørselTekster.lagSaksTittel("OLA NORDMANN", LocalDate.of(2021, 02, 1));
        assertEquals("Inntektsmelding for Ola Nordmann (01.02.21)", saksTittel);
    }

    @Test
    void lagOppgaveTekst() {
        String oppgaveTekst = ForespørselTekster.lagOppgaveTekst(Ytelsetype.OPPLÆRINGSPENGER);
        assertEquals("Innsending av inntektsmelding for opplæringspenger", oppgaveTekst);
    }

    @Test
    void lagTilleggsInformasjon_EksternInnsending() {
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.EKSTERN_INNSENDING);
        assertEquals("Utført i Altinn eller i bedriftens lønns- og personalsystem", statusTekst);
    }
    @Test
    void lagTilleggsInformasjon_OrdinærInnsending() {
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING);
        assertEquals(null, statusTekst);
    }

    @Test
    void lagTilleggsInformasjon_Utgått() {
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT);
        assertEquals("Du trenger ikke lenger å sende denne inntektsmeldingen", statusTekst);
    }
}
