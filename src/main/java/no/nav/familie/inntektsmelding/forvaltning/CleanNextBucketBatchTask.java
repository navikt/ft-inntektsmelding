package no.nav.familie.inntektsmelding.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "partition.cleanBucket", cronExpression = "0 0 0 1 * *", maxFailedRuns = 1)
public class CleanNextBucketBatchTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CleanNextBucketBatchTask.class);
    private final ProsessTaskTjeneste taskTjeneste;

    @Inject
    public CleanNextBucketBatchTask(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var antallSlettet = taskTjeneste.tømNestePartisjon();
        LOG.info("Tømmer neste partisjon med ferdige tasks, slettet {}", antallSlettet);
    }
}
