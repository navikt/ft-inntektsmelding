package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = SettForespørselTilUtgåttTask.TASKTYPE)
public class SettForespørselTilUtgåttTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forespørsel.utgått";
    private static final Logger log = LoggerFactory.getLogger(SettForespørselTilUtgåttTask.class);

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @Inject
    public SettForespørselTilUtgåttTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    SettForespørselTilUtgåttTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String forespørselUuid = prosessTaskData.getPropertyValue(FORESPØRSEL_UUID);
        Optional<ForespørselEntitet> opt = forespørselBehandlingTjeneste.hentForespørsel(UUID.fromString(forespørselUuid));

        if (opt.isEmpty()) {
            log.warn("Fant ikke forespørsel med uuid {}", forespørselUuid);
            return;
        }

        ForespørselEntitet forespørsel = opt.get();
        if (forespørsel.getStatus() == ForespørselStatus.UTGÅTT) {
            log.info("Forespørsel med uuid {} har allerede status utgått", forespørselUuid);
            return;
        }

        boolean skalOppdatereArbeidsgiverNotifikasjon = forespørsel.getStatus() == ForespørselStatus.UNDER_BEHANDLING;
        forespørselBehandlingTjeneste.settForespørselTilUtgått(forespørsel, skalOppdatereArbeidsgiverNotifikasjon);
        MetrikkerTjeneste.loggForespørselLukkEkstern(forespørsel);
    }
}
