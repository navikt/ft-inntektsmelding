package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.FantIkkeArbeidstakerException;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.InnsenderHarIkkeTilgangTilArbeidsforholdException;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.dto.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ArbeidstakerTjenesteTest {

    private static final PersonIdent PERSON_IDENT = PersonIdent.fra("21073926618");

    @Mock
    private PersonTjeneste personTjenesteMock;

    @Mock
    private ArbeidsforholdTjeneste arbeidsforholdTjenesteMock;

    @Mock
    private AltinnTilgangTjeneste altinnTilgangTjenesteMock;

    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    @BeforeEach
    void setUp() {
        this.arbeidstakerTjeneste = new ArbeidstakerTjeneste(personTjenesteMock, arbeidsforholdTjenesteMock, altinnTilgangTjenesteMock);
    }

    @Test
    void skal_kaste_exception_når_person_ikke_finnes() {
        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(null);

        assertThrows(FantIkkeArbeidstakerException.class,
            () -> arbeidstakerTjeneste.slåOppArbeidstaker(PERSON_IDENT));
    }

    @Test
    void skal_returnere_person_uten_arbeidsforhold() {
        var personInfo = new PersonInfo(
            "Test",
            "Mellom",
            "Testesen",
            PERSON_IDENT,
            AktørIdEntitet.dummy(),
            LocalDate.now(),
            null
        );

        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(personInfo);
        when(arbeidsforholdTjenesteMock.hentNåværendeArbeidsforhold(any())).thenReturn(Collections.emptyList());

        var resultat = arbeidstakerTjeneste.slåOppArbeidstaker(PERSON_IDENT);

        assertThat(resultat.fornavn()).isEqualTo("Test");
        assertThat(resultat.mellomnavn()).isEqualTo("Mellom");
        assertThat(resultat.etternavn()).isEqualTo("Testesen");
        assertTrue(resultat.arbeidsforhold().isEmpty());
    }

    @Test
    void skal_kaste_exception_når_innsender_ikke_har_tilgang() {
        var personInfo = new PersonInfo(
            "Test",
            "Mellom",
            "Testesen",
            PERSON_IDENT,
            AktørIdEntitet.dummy(),
            LocalDate.now(),
            null
        );

        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(personInfo);
        when(arbeidsforholdTjenesteMock.hentNåværendeArbeidsforhold(any())).thenReturn(
            List.of(new ArbeidsforholdDto("Arbeidsgiver AS", "123456789", "987654321"))
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(false);

        assertThrows(InnsenderHarIkkeTilgangTilArbeidsforholdException.class,
            () -> arbeidstakerTjeneste.slåOppArbeidstaker(PERSON_IDENT));
    }

    @Test
    void skal_returnere_kun_arbeidsforhold_med_tilgang() {
        var personInfo = new PersonInfo(
            "Test",
            "Mellom",
            "Testesen",
            PERSON_IDENT,
            AktørIdEntitet.dummy(),
            LocalDate.now(),
            null
        );

        var arbeidsforhold1 = new ArbeidsforholdDto("Arbeidsgiver 1", "123456789", "987654321");
        var arbeidsforhold2 = new ArbeidsforholdDto("Arbeidsgiver 2", "987654321", "123456789");

        when(personTjenesteMock.hentPersonFraIdent(any(), any())).thenReturn(personInfo);
        when(arbeidsforholdTjenesteMock.hentNåværendeArbeidsforhold(any()))
            .thenReturn(List.of(arbeidsforhold1, arbeidsforhold2));

        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("123456789")).thenReturn(false);
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("987654321")).thenReturn(true);

        var resultat = arbeidstakerTjeneste.slåOppArbeidstaker(PERSON_IDENT);

        assertThat(resultat.arbeidsforhold().size()).isEqualTo(1);
        assertThat(resultat.arbeidsforhold().getFirst().arbeidsgiver()).isEqualTo("Arbeidsgiver 2");
        assertThat(resultat.arbeidsforhold().getFirst().underenhetId()).isEqualTo("987654321");
    }
}
