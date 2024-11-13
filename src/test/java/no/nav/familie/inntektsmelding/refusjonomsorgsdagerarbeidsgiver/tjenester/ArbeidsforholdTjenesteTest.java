package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.OpplysningspliktigArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.AaregRestKlient;
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
    when(aaregRestKlient.finnArbeidsforholdForArbeidstaker(eq(PERSON_IDENT.getIdent()), any(LocalDate.class),
        any(LocalDate.class)))
        .thenReturn(null);

    var resultat = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(PERSON_IDENT);

    assertThat(resultat).isEmpty();
  }

  @Test
  void skalReturnereTomListeNårAaregReturnerTomListe() {
    when(aaregRestKlient.finnArbeidsforholdForArbeidstaker(eq(PERSON_IDENT.getIdent()), any(LocalDate.class),
        any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    var resultat = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(PERSON_IDENT);

    assertThat(resultat).isEmpty();
  }

  @Test
  void skalMappeArbeidsforholdKorrekt() {
    var arbeidsforhold = new ArbeidsforholdDto(
        "abc123",
        123L,
        new OpplysningspliktigArbeidsgiverDto(
            OpplysningspliktigArbeidsgiverDto.Type.Organisasjon,
            "999999999",
            "000000000",
            "Arbeidsgiver AS"
        ),
        null,
        null,
        null,
        "type"
    );

    when(aaregRestKlient.finnArbeidsforholdForArbeidstaker(eq(PERSON_IDENT.getIdent()), any(LocalDate.class),
        any(LocalDate.class)))
        .thenReturn(List.of(arbeidsforhold));

    var resultat = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(PERSON_IDENT);

    assertThat(resultat)
        .hasSize(1)
        .first()
        .satisfies(dto -> {
          assertThat(dto.underenhetId()).isEqualTo("999999999");
          assertThat(dto.arbeidsforholdId()).isEqualTo("abc123");
          assertThat(dto.arbeidsgiver()).isEqualTo("Arbeidsgiver AS");
        });
  }

  @Test
  void skalMappeFlereArbeidsforholdKorrekt() {
      var arbeidsforhold1 = new ArbeidsforholdDto(
          "arbeidsforhold id 1",
          123L,
          new OpplysningspliktigArbeidsgiverDto(
              OpplysningspliktigArbeidsgiverDto.Type.Organisasjon,
              "000000001",
              "100000001",
              "Eino Arbeidsgiver AS"
          ),
          null,
          null,
          null,
          "type"
      );

      var arbeidsforhold2 = new ArbeidsforholdDto(
          "arbeidsforhold id 2",
          123L,
          new OpplysningspliktigArbeidsgiverDto(
              OpplysningspliktigArbeidsgiverDto.Type.Organisasjon,
              "000000002",
              "100000002",
              "André Arbeidsgiver AS"
          ),
          null,
          null,
          null,
          "type"
      );

    when(aaregRestKlient.finnArbeidsforholdForArbeidstaker(eq(PERSON_IDENT.getIdent()), any(LocalDate.class),
        any(LocalDate.class)))
        .thenReturn(List.of(arbeidsforhold1, arbeidsforhold2));

    var resultat = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(PERSON_IDENT);

    assertThat(resultat).hasSize(2);

    assertThat(resultat.getFirst().underenhetId()).isEqualTo("000000001");
    assertThat(resultat.getFirst().arbeidsforholdId()).isEqualTo("arbeidsforhold id 1");
    assertThat(resultat.getFirst().arbeidsgiver()).isEqualTo("Eino Arbeidsgiver AS");
    assertThat(resultat.get(1).underenhetId()).isEqualTo("000000002");
    assertThat(resultat.get(1).arbeidsforholdId()).isEqualTo("arbeidsforhold id 2");
    assertThat(resultat.get(1).arbeidsgiver()).isEqualTo("André Arbeidsgiver AS");
  }
}
