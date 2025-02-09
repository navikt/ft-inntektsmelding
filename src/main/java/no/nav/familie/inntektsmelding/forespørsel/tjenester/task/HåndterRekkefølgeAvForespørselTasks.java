package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskLifecycleObserver;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.ProsessTaskVeto;
import no.nav.k9.prosesstask.api.TaskType;

@ApplicationScoped
public class HåndterRekkefølgeAvForespørselTasks implements ProsessTaskLifecycleObserver {

    private static final Logger LOG = LoggerFactory.getLogger(HåndterRekkefølgeAvForespørselTasks.class);

    private static final TaskType OPPRETT = TaskType.forProsessTask(OpprettForespørselTask.class);

    private static final Set<TaskType> BLOKKERENDE = Set.of(
        OPPRETT,
        TaskType.forProsessTask(SettForespørselTilUtgåttTask.class),
        TaskType.forProsessTask(GjenåpneForespørselTask.class));

    private ProsessTaskTjeneste prosessTaskTjeneste;

    HåndterRekkefølgeAvForespørselTasks() {
        //CDI
    }

    @Inject
    public HåndterRekkefølgeAvForespørselTasks(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public ProsessTaskVeto vetoKjøring(ProsessTaskData prosessTaskData) {

        if (prosessTaskData.taskType().equals(OPPRETT)) {
            var fagsakSaksnummer = prosessTaskData.getSaksnummer();

            if (fagsakSaksnummer == null || fagsakSaksnummer.isBlank()) {
                throw new IllegalArgumentException("Task av type " + OPPRETT.value() + " mangler saksnummer");
            }

            //TODO bytt til en mer spesifikk query
            Optional<ProsessTaskData> blokkerendeTask = prosessTaskTjeneste.finnAlle(ProsessTaskStatus.KLAR)
                .stream()
                .filter(task -> BLOKKERENDE.contains(task.taskType()))
                .filter(task -> Objects.equals(task.getSaksnummer(), fagsakSaksnummer))
                .filter(task -> !Objects.equals(task.getGruppe(), prosessTaskData.getGruppe()))
                .filter(task -> task.getOpprettetTid().isBefore(prosessTaskData.getOpprettetTid()))
                .max(Comparator.comparing(ProsessTaskData::getOpprettetTid));

            if (blokkerendeTask.isPresent()) {
                LOG.info("Vetoer kjøring av prosesstask[{}] av {} for fagsak [{}], er blokkert av prosesstask[{}] for samme fagsak.",
                    prosessTaskData.getId(), prosessTaskData.taskType(), fagsakSaksnummer, blokkerendeTask.get().getId());

                return new ProsessTaskVeto(true, prosessTaskData.getId(), blokkerendeTask.get().getId(),
                    "Må vente på annen task for samme fagsak som ble opprettet før denne.");
            }
        }

        return new ProsessTaskVeto(false, prosessTaskData.getId());
    }

    @Override
    public void opprettetProsessTaskGruppe(ProsessTaskGruppe sammensattTask) {

    }
}
