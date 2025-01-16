package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.AaregRestKlient;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

@ExtendWith(MockitoExtension.class)
class ArbeidsforholdTjenesteTest {

    @Mock
    private AaregRestKlient aaregRestKlient;

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    private static final PersonIdent PERSON_IDENT = PersonIdent.fra("12345678901");

    @BeforeEach
    void setUp() {
        arbeidsforholdTjeneste = new ArbeidsforholdTjeneste(aaregRestKlient);
    }

    @Test
    void skalReturnereTomListeNårAaregReturnerNull() {
        when(aaregRestKlient.finnNåværendeArbeidsforholdForArbeidstaker(PERSON_IDENT.getIdent()))
            .thenReturn(null);

        var resultat = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(PERSON_IDENT);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skalReturnereTomListeNårAaregReturnerTomListe() {
        when(aaregRestKlient.finnNåværendeArbeidsforholdForArbeidstaker(PERSON_IDENT.getIdent()))
            .thenReturn(Collections.emptyList());

        var resultat = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(PERSON_IDENT);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skalMappeArbeidsforholdKorrekt() {
        var arbeidsforhold = new ArbeidsforholdDto(
            "abc123",
            123L,
            new ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto(
                ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Type.Organisasjon,
                List.of(
                    new ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Ident(ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Ident.Type.ORGANISASJONSNUMMER, "999999999", true)
                )

            ),
            new ArbeidsforholdDto.AnsettelsesperiodeDto(LocalDate.now(),LocalDate.now()),
            "type"
        );

        when(aaregRestKlient.finnNåværendeArbeidsforholdForArbeidstaker(PERSON_IDENT.getIdent()))
            .thenReturn(List.of(arbeidsforhold));

        var resultat = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(PERSON_IDENT);

        assertThat(resultat)
            .hasSize(1)
            .first()
            .satisfies(dto -> {
                assertThat(dto.underenhetId()).isEqualTo("999999999");
                assertThat(dto.arbeidsforholdId()).isEqualTo("123");
//                assertThat(dto.arbeidsgiver()).isEqualTo("Arbeidsgiver AS");
            });
    }

    @Test
    void skalMappeFlereArbeidsforholdKorrekt() {
        var arbeidsforhold1 = new ArbeidsforholdDto(
            "arbeidsforhold id 1",
            1L,
            new ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto(
                ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Type.Organisasjon,
                List.of(
                    new ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Ident(ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Ident.Type.ORGANISASJONSNUMMER, "000000001", true)
                )

            ),
            new ArbeidsforholdDto.AnsettelsesperiodeDto(LocalDate.now(),LocalDate.now()),
            "type"
        );

        var arbeidsforhold2 = new ArbeidsforholdDto(
            "arbeidsforhold id 2",
            2L,
            new ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto(
                ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Type.Organisasjon,
                List.of(
                    new ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Ident(ArbeidsforholdDto.OpplysningspliktigArbeidsgiverDto.Ident.Type.ORGANISASJONSNUMMER, "000000002", true)
                )

            ),
            new ArbeidsforholdDto.AnsettelsesperiodeDto(LocalDate.now(),LocalDate.now()),
            "type"
        );


        when(aaregRestKlient.finnNåværendeArbeidsforholdForArbeidstaker(PERSON_IDENT.getIdent()))
            .thenReturn(List.of(arbeidsforhold1, arbeidsforhold2));

        var resultat = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(PERSON_IDENT);

        assertThat(resultat).hasSize(2);

        assertThat(resultat.getFirst().underenhetId()).isEqualTo("000000001");
        assertThat(resultat.getFirst().arbeidsforholdId()).isEqualTo("1");
//        assertThat(resultat.getFirst().arbeidsgiver()).isEqualTo("Eino Arbeidsgiver AS");
        assertThat(resultat.get(1).underenhetId()).isEqualTo("000000002");
        assertThat(resultat.get(1).arbeidsforholdId()).isEqualTo("2");
//        assertThat(resultat.get(1).arbeidsgiver()).isEqualTo("André Arbeidsgiver AS");
    }
}
