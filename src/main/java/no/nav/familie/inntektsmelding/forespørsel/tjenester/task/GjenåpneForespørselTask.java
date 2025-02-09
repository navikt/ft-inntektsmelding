package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("forespørsel.gjenåpne")
public class GjenåpneForespørselTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GjenåpneForespørselTask.class);

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    @Inject
    public GjenåpneForespørselTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                   InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    GjenåpneForespørselTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String forespørselUuid = prosessTaskData.getPropertyValue(FORESPØRSEL_UUID);
        Optional<ForespørselEntitet> opt = forespørselBehandlingTjeneste.hentForespørsel(UUID.fromString(forespørselUuid));

        if (opt.isEmpty()) {
            LOG.warn("Fant ikke forespørsel med uuid {}", forespørselUuid);
            return;
        }

        ForespørselEntitet forespørsel = opt.get();
        if (forespørsel.getStatus() != ForespørselStatus.UTGÅTT) {
            LOG.info("Forespørsel med uuid {} er allerede åpen", forespørselUuid);
            return;
        }

        List<InntektsmeldingResponseDto> inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(UUID.fromString(forespørselUuid));
        if (inntektsmeldinger.isEmpty()) {
            throw new IllegalArgumentException("Kan ikke gjenåpne forespørsel som ikke har fått inn inntektsmelding");
        }

        forespørselBehandlingTjeneste.gjenåpneForespørsel(forespørsel);
        //TODO metrikker?
    }
}
