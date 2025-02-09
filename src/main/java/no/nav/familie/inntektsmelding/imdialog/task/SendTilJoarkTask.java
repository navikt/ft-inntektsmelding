package no.nav.familie.inntektsmelding.imdialog.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.K9DokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.joark.JoarkTjeneste;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "mottaInntektsmelding.oversendJoark")
public class SendTilJoarkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SendTilJoarkTask.class);
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste;
    private K9DokgenTjeneste k9DokgenTjeneste;
    private JoarkTjeneste joarkTjeneste;

    SendTilJoarkTask() {
        // CDI
    }

    @Inject
    public SendTilJoarkTask(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                            InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste,
                            K9DokgenTjeneste k9DokgenTjeneste,
                            JoarkTjeneste joarkTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingXMLTjeneste = inntektsmeldingXMLTjeneste;
        this.k9DokgenTjeneste = k9DokgenTjeneste;
        this.joarkTjeneste = joarkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var inntektsmeldingId = Integer.parseInt(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        var fagsysteSaksnummer = prosessTaskData.getSaksnummer();
        LOG.info("Starter task for oversending til joark for saksnummer {}", fagsysteSaksnummer);

        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        var xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmelding);

        var pdf = k9DokgenTjeneste.mapDataOgGenererPdf(inntektsmelding);

        LOG.debug("Genererte XML: {} og pdf av inntektsmeldingen, journalfører på sak: {}", xml, fagsysteSaksnummer);
        joarkTjeneste.journalførInntektsmelding(xml, inntektsmelding, pdf, fagsysteSaksnummer);
        LOG.info("Sluttfører task oversendJoark");
    }
}
