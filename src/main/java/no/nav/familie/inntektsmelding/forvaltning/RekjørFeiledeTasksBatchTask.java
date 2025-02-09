package no.nav.familie.inntektsmelding.forvaltning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = RekjørFeiledeTasksBatchTask.TASKTYPE, cronExpression = "0 30 7,12,17 * * *")
public class RekjørFeiledeTasksBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.retryFeilendeTasks";
    private static final Logger log = LoggerFactory.getLogger(RekjørFeiledeTasksBatchTask.class);
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public RekjørFeiledeTasksBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        int rekjørAlleFeiledeTasks = prosessTaskTjeneste.restartAlleFeiledeTasks();
        log.info("Rekjører alle feilende tasks, oppdaterte {} tasks", rekjørAlleFeiledeTasks);
    }
}
