package no.nav.familie.inntektsmelding.imdialog.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingDialogTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "mottaInntektsmelding.oversendJoark")
public class SendTilJoarkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SendTilJoarkTask.class);
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";

    private InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste;
    private InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste;

    SendTilJoarkTask() {
        // CDI
    }

    @Inject
    public SendTilJoarkTask(InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste,
                            InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste) {
        this.inntektsmeldingDialogTjeneste = inntektsmeldingDialogTjeneste;
        this.inntektsmeldingXMLTjeneste = inntektsmeldingXMLTjeneste;

    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("Opprettet task for oversending til joark");
        var inntektsmeldingId = Integer.parseInt(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        var inntektsmelding = inntektsmeldingDialogTjeneste.hentInntektsmelding(inntektsmeldingId);
        var xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmelding);
        LOG.info("Genererte XML " + xml);
    }
}
