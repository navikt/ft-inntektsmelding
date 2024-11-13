package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;

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

    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    @BeforeEach
    void setUp() {
        this.arbeidstakerTjeneste = new ArbeidstakerTjeneste(this.personTjenesteMock, this.arbeidsforholdTjenesteMock);
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

        var resultat = arbeidstakerTjeneste.slåOppArbeidstaker(TILFELDIG_PERSON_IDENT);

        assertThat(resultat.arbeidsforhold().size()).isEqualTo(1);
        var arbeidsforhold = resultat.arbeidsforhold().get(0);

        assertThat(arbeidsforhold.arbeidsgiver()).isEqualTo("Dummy arbeidsgiver");
        assertThat(arbeidsforhold.underenhetId()).isEqualTo("00000000");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("123456789");
    }

}
