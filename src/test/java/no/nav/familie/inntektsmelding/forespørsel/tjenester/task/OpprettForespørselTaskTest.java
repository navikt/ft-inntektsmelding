package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.prosesstask.api.ProsessTaskData;

class OpprettForespørselTaskTest {

    private final Ytelsetype ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
    private final AktørIdEntitet aktørId = new AktørIdEntitet("1111111111111");
    private final SaksnummerDto fagsakSaksnummer = new SaksnummerDto("456");
    private final OrganisasjonsnummerDto organisasjon = new OrganisasjonsnummerDto("789");
    private final LocalDate skjæringstidspunkt = LocalDate.now();

    private final ForespørselBehandlingTjeneste forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);

    @Test
    void skal_opprette_forespørsel_dersom_det_ikke_eksisterer_en_for_stp() {
        var task = new OpprettForespørselTask(forespørselBehandlingTjeneste);
        var taskdata = ProsessTaskData.forProsessTask(OpprettForespørselTask.class);
        taskdata.setProperty(OpprettForespørselTask.YTELSETYPE, ytelsetype.name());
        taskdata.setAktørId(aktørId.getAktørId());
        taskdata.setSaksnummer(fagsakSaksnummer.saksnr());
        taskdata.setProperty(OpprettForespørselTask.ORGNR, organisasjon.orgnr());
        taskdata.setProperty(OpprettForespørselTask.STP, skjæringstidspunkt.toString());

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste).opprettForespørsel(ytelsetype, aktørId, fagsakSaksnummer, organisasjon, skjæringstidspunkt, null);
    }

    @Test
    void skal_ikke_opprette_ny_forespørsel_dersom_det_eksisterer_en_for_samme_stp() {
        var task = new OpprettForespørselTask(forespørselBehandlingTjeneste);
        var taskdata = ProsessTaskData.forProsessTask(OpprettForespørselTask.class);
        taskdata.setProperty(OpprettForespørselTask.YTELSETYPE, ytelsetype.name());
        taskdata.setAktørId(aktørId.getAktørId());
        taskdata.setSaksnummer(fagsakSaksnummer.saksnr());
        taskdata.setProperty(OpprettForespørselTask.ORGNR, organisasjon.orgnr());
        taskdata.setProperty(OpprettForespørselTask.STP, skjæringstidspunkt.toString());

        when(forespørselBehandlingTjeneste.hentForespørslerForFagsak(fagsakSaksnummer, organisasjon, skjæringstidspunkt))
            .thenReturn(List.of(new ForespørselEntitet(organisasjon.orgnr(), skjæringstidspunkt, aktørId, ytelsetype, fagsakSaksnummer.saksnr(), null
            )));

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste, times(0)).opprettForespørsel(any(), any(), any(), any(), any(), any());
    }
}
