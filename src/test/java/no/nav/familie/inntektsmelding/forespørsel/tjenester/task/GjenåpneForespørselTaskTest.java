package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.k9.prosesstask.api.ProsessTaskData;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GjenåpneForespørselTaskTest {

    private final UUID forespørselUuid = UUID.randomUUID();
    private final ForespørselEntitet entitet = new ForespørselEntitet();
    private final InntektsmeldingResponseDto inntektsmeldingResponseDto = new InntektsmeldingResponseDto(
        1L,
        UUID.randomUUID(),
        mock(AktørIdDto.class),
        mock(YtelseTypeDto.class),
        mock(ArbeidsgiverDto.class),
        mock(SendInntektsmeldingRequestDto.KontaktpersonRequestDto.class),
        LocalDate.now(),
        new BigDecimal("50000.00"),
        LocalDateTime.now(),
        List.of(mock(SendInntektsmeldingRequestDto.Refusjon.class)),
        List.of(mock(SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto.class)),
        List.of(mock(SendInntektsmeldingRequestDto.EndringsårsakerRequestDto.class))
    );

    private final ForespørselBehandlingTjeneste forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = Mockito.mock(InntektsmeldingTjeneste.class);

    @Test
    void skal_gjenåpne_forespørsel() {
        var task = new GjenåpneForespørselTask(forespørselBehandlingTjeneste, inntektsmeldingTjeneste);
        var taskdata = ProsessTaskData.forProsessTask(GjenåpneForespørselTask.class);
        taskdata.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, forespørselUuid.toString());

        entitet.setStatus(ForespørselStatus.UTGÅTT);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(entitet));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid)).thenReturn(List.of(inntektsmeldingResponseDto));

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste).gjenåpneForespørsel(entitet);
    }

    @Test
    void skal_ikke_gjenåpne_dersom_status_er_under_behandling() {
        var task = new GjenåpneForespørselTask(forespørselBehandlingTjeneste, inntektsmeldingTjeneste);
        var taskdata = ProsessTaskData.forProsessTask(GjenåpneForespørselTask.class);
        taskdata.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, forespørselUuid.toString());

        entitet.setStatus(ForespørselStatus.UNDER_BEHANDLING);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(entitet));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid)).thenReturn(List.of(inntektsmeldingResponseDto));

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste, times(0)).gjenåpneForespørsel(entitet);
    }

    @Test
    void skal_ikke_gjenåpne_dersom_status_er_ferdig() {
        var task = new GjenåpneForespørselTask(forespørselBehandlingTjeneste, inntektsmeldingTjeneste);
        var taskdata = ProsessTaskData.forProsessTask(GjenåpneForespørselTask.class);
        taskdata.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, forespørselUuid.toString());

        entitet.setStatus(ForespørselStatus.FERDIG);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(entitet));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid)).thenReturn(List.of(inntektsmeldingResponseDto));

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste, times(0)).gjenåpneForespørsel(entitet);
    }

    @Test
    void skal_kaste_feil_dersom_vi_ikke_finner_im() {
        var task = new GjenåpneForespørselTask(forespørselBehandlingTjeneste, inntektsmeldingTjeneste);
        var taskdata = ProsessTaskData.forProsessTask(GjenåpneForespørselTask.class);
        taskdata.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, forespørselUuid.toString());

        entitet.setStatus(ForespørselStatus.UTGÅTT);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(entitet));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid)).thenReturn(List.of());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> task.doTask(taskdata));
        assertEquals("Kan ikke gjenåpne forespørsel som ikke har fått inn inntektsmelding", exception.getMessage());

        verify(forespørselBehandlingTjeneste, times(0)).gjenåpneForespørsel(entitet);
    }
}
