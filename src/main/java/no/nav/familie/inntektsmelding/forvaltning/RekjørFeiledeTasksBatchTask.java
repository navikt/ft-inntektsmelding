package no.nav.familie.inntektsmelding.forvaltning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "batch.retryFeilendeTasks", cronExpression = "0 30 7,12,17 * * *")
public class RekjørFeiledeTasksBatchTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(RekjørFeiledeTasksBatchTask.class);
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public RekjørFeiledeTasksBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        int antall = prosessTaskTjeneste.restartAlleFeiledeTasks();
        log.info("Rekjører alle feilende tasks, oppdaterte {} tasks", antall);
    }
}
