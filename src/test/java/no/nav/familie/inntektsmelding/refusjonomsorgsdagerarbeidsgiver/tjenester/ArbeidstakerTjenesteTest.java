package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;

import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ArbeidstakerTjenesteTest {

    private static final PersonIdent TILFELDIG_PERSON_IDENT = PersonIdent.fra("21073926618");

    @Mock
    private PersonTjeneste personTjenesteMock;

    @Mock
    private ArbeidsforholdTjeneste arbeidsforholdTjenesteMock;

    @Mock
    private AltinnTilgangTjeneste altinnTilgangTjenesteMock;

    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    @BeforeEach
    void setUp() {
        this.arbeidstakerTjeneste = new ArbeidstakerTjeneste(this.personTjenesteMock, this.arbeidsforholdTjenesteMock, this.altinnTilgangTjenesteMock);
    }

    @Test
    public void returnerer_null_om_pdl_ikke_finner_personen() {
        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(null);
        assertThat(arbeidstakerTjeneste.slåOppArbeidstaker(TILFELDIG_PERSON_IDENT)).isNull();
    }

    @Test
    public void returnerer_arbeidstakerinfo_om_pdl_finner_personen() {
        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(
            new PersonInfo("Test", "Filiokus", "Personesen", TILFELDIG_PERSON_IDENT, AktørIdEntitet.dummy(), LocalDate.now(), null)
        );
        when(arbeidsforholdTjenesteMock.hentNåværendeArbeidsforhold(any())).thenReturn(
            List.of(new ArbeidsforholdDto("Dummy arbeid", "000000000", "111111111"))
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(true);

        var resultat = arbeidstakerTjeneste.slåOppArbeidstaker(TILFELDIG_PERSON_IDENT);
        assertThat(resultat).isNotNull();
        assertThat(resultat.fornavn()).isEqualTo("Test");
        assertThat(resultat.mellomnavn()).isEqualTo("Filiokus");
        assertThat(resultat.etternavn()).isEqualTo("Personesen");
        assertThat(resultat.arbeidsforhold().size()).isEqualTo(1);

        var arbeidsforhold = resultat.arbeidsforhold().getFirst();
        assertThat(arbeidsforhold.arbeidsgiver()).isEqualTo("Dummy arbeid");
        assertThat(arbeidsforhold.underenhetId()).isEqualTo("000000000");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("111111111");
    }

    @Test
    public void returnerer_arbeidstakerinfo_uten_mellomnavn() {
        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(
            new PersonInfo("Test", null, "Personesen", TILFELDIG_PERSON_IDENT, AktørIdEntitet.dummy(), LocalDate.now(), null)
        );

        var resultat = arbeidstakerTjeneste.slåOppArbeidstaker(TILFELDIG_PERSON_IDENT);

        assertThat(resultat).isNotNull();
        assertThat(resultat.fornavn()).isEqualTo("Test");
        assertThat(resultat.mellomnavn()).isNull();
        assertThat(resultat.etternavn()).isEqualTo("Personesen");
    }

    @Test
    public void verifiserer_arbeidsforhold_detaljer() {
        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(
            new PersonInfo("Test", "Filiokus", "Personesen", TILFELDIG_PERSON_IDENT, AktørIdEntitet.dummy(), LocalDate.now(), null)
        );
        when(arbeidsforholdTjenesteMock.hentNåværendeArbeidsforhold(any())).thenReturn(
            List.of(new ArbeidsforholdDto("Dummy arbeidsgiver", "00000000", "123456789")));
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(true);

        var resultat = arbeidstakerTjeneste.slåOppArbeidstaker(TILFELDIG_PERSON_IDENT);

        assertThat(resultat.arbeidsforhold().size()).isEqualTo(1);
        var arbeidsforhold = resultat.arbeidsforhold().get(0);

        assertThat(arbeidsforhold.arbeidsgiver()).isEqualTo("Dummy arbeidsgiver");
        assertThat(arbeidsforhold.underenhetId()).isEqualTo("00000000");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("123456789");
    }

    @Test
    public void filtrerer_ut_arbeidsforhold_man_ikke_har_tilgang_til() {
        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(
            new PersonInfo("Test", "Filiokus", "Personesen", TILFELDIG_PERSON_IDENT, AktørIdEntitet.dummy(), LocalDate.now(), null)
        );
        when(arbeidsforholdTjenesteMock.hentNåværendeArbeidsforhold(any())).thenReturn(
            List.of(
                new ArbeidsforholdDto("Dummy arbeidsgiver", "00000000", "123456789"),
                new ArbeidsforholdDto("Dummy arbeidsgiver", "00000001", "123456789")
            )
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("00000000")).thenReturn(false);
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("00000001")).thenReturn(true);

        var resultat = arbeidstakerTjeneste.slåOppArbeidstaker(TILFELDIG_PERSON_IDENT);

        assertThat(resultat.arbeidsforhold().size()).isEqualTo(1);
        var arbeidsforhold = resultat.arbeidsforhold().getFirst();

        assertThat(arbeidsforhold.arbeidsgiver()).isEqualTo("Dummy arbeidsgiver");
        assertThat(arbeidsforhold.underenhetId()).isEqualTo("00000001");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("123456789");
    }
}
