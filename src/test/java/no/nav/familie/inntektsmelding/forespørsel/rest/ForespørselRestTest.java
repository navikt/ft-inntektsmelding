package no.nav.familie.inntektsmelding.forespørsel.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ExtendWith(MockitoExtension.class)
public class ForespørselRestTest {

    private static final String BRREG_ORGNUMMER = "974760673";

    private ForespørselRest forespørselRest;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private Tilgang tilgang;

    @BeforeEach
    void setUp() {
        this.forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);
        this.forespørselRest = new ForespørselRest(forespørselBehandlingTjeneste, tilgang);
    }

    @Test
    void skal_opprette_forespørsel() {
        mockForespørsel();

        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var aktørId = new AktørIdDto("1234567890134");

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.opprettForespørsel(
            new OpprettForespørselRequest(aktørId, orgnummer, LocalDate.now(), YtelseTypeDto.PLEIEPENGER_SYKT_BARN, fagsakSaksnummer, LocalDate.now().plusDays(5)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(new OpprettForespørselResponse(ForespørselResultat.FORESPØRSEL_OPPRETTET));
        verify(forespørselBehandlingTjeneste).håndterInnkommendeForespørsel(eq(LocalDate.now()), eq(Ytelsetype.PLEIEPENGER_SYKT_BARN),
            eq(new AktørIdEntitet(aktørId.id())), eq(orgnummer), eq(fagsakSaksnummer), eq(LocalDate.now().plusDays(5)));
    }

    @Test
    void serdes_forespørsel_mapper() {
        var expectedOrg = "123456789";
        var expectedBruker = "1233425324241";
        var expectedSkjæringstidspunkt = LocalDate.now();
        var input = new ForespørselEntitet(expectedOrg, expectedSkjæringstidspunkt, new AktørIdEntitet(expectedBruker), Ytelsetype.FORELDREPENGER,
            "9876544321", expectedSkjæringstidspunkt.plusDays(10));

        var resultat = ForespørselRest.mapTilDto(input);

        assertThat(resultat).isNotNull().isInstanceOf(ForespørselRest.ForespørselDto.class);
        assertThat(resultat.organisasjonsnummer()).isEqualTo(new OrganisasjonsnummerDto(expectedOrg));
        assertThat(resultat.skjæringstidspunkt()).isEqualTo(expectedSkjæringstidspunkt);
        assertThat(resultat.brukerAktørId()).isEqualTo(new AktørIdDto(expectedBruker));
        assertThat(resultat.ytelseType()).isEqualTo(YtelseTypeDto.FORELDREPENGER);
        assertThat(resultat.uuid()).isNotNull();
    }

    @Test
    void serdes() {
        var expectedOrg = new OrganisasjonsnummerDto("123456789");
        var expectedBruker = new AktørIdDto("123342532424");
        var expectedSkjæringstidspunkt = LocalDate.now();
        var dto = new ForespørselRest.ForespørselDto(UUID.randomUUID(), expectedOrg, expectedSkjæringstidspunkt, expectedBruker,
            YtelseTypeDto.SVANGERSKAPSPENGER);

        var ser = DefaultJsonMapper.toJson(dto);
        var des = DefaultJsonMapper.fromJson(ser, ForespørselRest.ForespørselDto.class);


        assertThat(ser).contains(expectedOrg.orgnr(), expectedBruker.id(), expectedSkjæringstidspunkt.toString());
        assertThat(des).isEqualTo(dto);
    }

    private void mockForespørsel() {
        when(forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(any(),
            any(),
            any(),
            any(),
            any(),
            any())).thenReturn(ForespørselResultat.FORESPØRSEL_OPPRETTET);
    }
}
