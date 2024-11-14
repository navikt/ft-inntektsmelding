package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;

@ExtendWith(MockitoExtension.class)
class AaregRestKlientTest {

    @Mock
    private RestClient restClient;

    private AaregRestKlient aaregRestKlient;

    @BeforeEach
    void setUp() {
        this.aaregRestKlient = new AaregRestKlient(restClient);
    }

    @Test
    void skal_hente_arbeidsforhold_for_person() {
        var ident = "12345678901";
        var fom = LocalDate.of(2024, 1, 1);
        var tom = LocalDate.of(2024, 3, 31);

        var arbeidsforhold = new ArbeidsforholdDto(
            "123",
            1234L,
            null,
            null,
            null,
            null,
            "ordinært");

        when(restClient.send(any(RestRequest.class), eq(ArbeidsforholdDto[].class)))
            .thenReturn(new ArbeidsforholdDto[]{arbeidsforhold});

        var result = aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident, fom, tom);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(arbeidsforhold);
        assertTrue(result.getFirst().arbeidsavtaler().isEmpty());
        assertTrue(result.getFirst().permisjonPermitteringer().isEmpty());

        var requestCaptor = ArgumentCaptor.forClass(RestRequest.class);
        verify(restClient).send(requestCaptor.capture(), eq(ArbeidsforholdDto[].class));
    }

    @Test
    void skal_kaste_exception_ved_ugyldig_uri() {
        // Arrange
        var ident = "12345678901";
        var fom = LocalDate.of(2024, 1, 1);
        var tom = LocalDate.of(2024, 3, 31);

        when(restClient.send(any(RestRequest.class), eq(ArbeidsforholdDto[].class)))
            .thenThrow(new IllegalArgumentException("Invalid URI"));

        // Act & Assert
        var exception = assertThrows(IllegalArgumentException.class,
            () -> aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident, fom, tom));

        assertThat(exception.getMessage())
            .isEqualTo("Utviklerfeil syntax-exception for finnArbeidsforholdForArbeidstaker");
    }

    @Test
    void skal_returnere_tom_liste_ved_ingen_arbeidsforhold() {
        // Arrange
        var ident = "12345678901";
        var fom = LocalDate.of(2024, 1, 1);
        var tom = LocalDate.of(2024, 3, 31);

        when(restClient.send(any(RestRequest.class), eq(ArbeidsforholdDto[].class)))
            .thenReturn(new ArbeidsforholdDto[]{});

        // Act
        var result = aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident, fom, tom);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void skal_bygge_korrekt_uri_for_arbeidsforhold() {
        // Arrange
        var fom = LocalDate.of(2024, 1, 1);
        var tom = LocalDate.of(2024, 3, 31);

        // Act
        var uri = aaregRestKlient.lagUriForForFinnArbeidsforholdForArbeidstaker(fom, tom);

        // Assert
        assertThat(uri.getPath()).endsWith("/arbeidsforhold");
        assertThat(uri.getQuery())
            .contains("ansettelsesperiodeFom=2024-01-01")
            .contains("ansettelsesperiodeTom=2024-03-31")
            .contains("regelverk=A_ORDNINGEN")
            .contains("historikk=true")
            .contains("sporingsinformasjon=false");
    }
}
