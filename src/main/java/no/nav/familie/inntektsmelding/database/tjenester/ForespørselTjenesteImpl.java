package no.nav.familie.inntektsmelding.database.tjenester;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.database.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.database.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørId;
import no.nav.familie.inntektsmelding.typer.FagsakSaksnummer;
import no.nav.familie.inntektsmelding.typer.Organisasjonsnummer;

@ApplicationScoped
@Default
public class ForespørselTjenesteImpl implements ForespørselTjeneste {

    private ForespørselRepository forespørselRepository;

    @Inject
    public ForespørselTjenesteImpl(ForespørselRepository forespørselRepository) {
        this.forespørselRepository = forespørselRepository;
    }

    public ForespørselTjenesteImpl() {
    }

    @Override
    public UUID opprettForespørsel(LocalDate skjæringstidspunkt,
                                   Ytelsetype ytelseType,
                                   AktørId brukerAktørId,
                                   Organisasjonsnummer orgnr,
                                   FagsakSaksnummer fagsakSaksnummer) {
        return forespørselRepository.lagreForespørsel(skjæringstidspunkt, ytelseType, brukerAktørId.getId(), orgnr.getOrgnr(),
            fagsakSaksnummer.getSaksnr());
    }


    @Override
    public void setOppgaveId(UUID forespørselUUID, String oppgaveId) {
        forespørselRepository.oppdaterOppgaveId(forespørselUUID, oppgaveId);
    }

    @Override
    public void setSakId(UUID forespørselUUID, String sakId) {
        forespørselRepository.oppdaterSakId(forespørselUUID, sakId);

    }

    @Override
    public Optional<ForespørselEntitet> finnForespørsel(String aktørId, String arbeidsgiverIdent, LocalDate startdato) {
        return forespørselRepository.finnForespørsel(aktørId, arbeidsgiverIdent, startdato);
    }

}
