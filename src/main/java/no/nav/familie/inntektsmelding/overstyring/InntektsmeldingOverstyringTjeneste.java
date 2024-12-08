package no.nav.familie.inntektsmelding.overstyring;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
public class InntektsmeldingOverstyringTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingOverstyringTjeneste.class);

    private InntektsmeldingRepository inntektsmeldingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    InntektsmeldingOverstyringTjeneste() {
        // CDI proxy
    }

    @Inject
    public InntektsmeldingOverstyringTjeneste(InntektsmeldingRepository inntektsmeldingRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void mottaOverstyrtInntektsmelding(SendOverstyrtInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        lagreOgLagJournalførTask(InntektsmeldingOverstyringMapper.mapTilEntitet(mottattInntektsmeldingDto), mottattInntektsmeldingDto.fagsystemSaksnummer());
    }

    private void lagreOgLagJournalførTask(InntektsmeldingEntitet entitet, SaksnummerDto fagsystemSaksnummer) {
        opprettTaskForSendTilJoark(inntektsmeldingRepository.lagreInntektsmelding(entitet), fagsystemSaksnummer);
    }

    private void opprettTaskForSendTilJoark(Long imId, SaksnummerDto fagsystemSaksnummer) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        task.setSaksnummer(fagsystemSaksnummer.saksnr());
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }
}
