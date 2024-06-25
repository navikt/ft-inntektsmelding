package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselOgSakRepository;
import no.nav.familie.inntektsmelding.forespørsel.modell.SakEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
public class ForespørselTjeneste {

    private ForespørselOgSakRepository forespørselRepository;

    @Inject
    public ForespørselTjeneste(ForespørselOgSakRepository forespørselRepository) {
        this.forespørselRepository = forespørselRepository;
    }

    public ForespørselTjeneste() {
    }


    public Optional<SakEntitet> finnSakUnderBehandling(Ytelsetype ytelseType,
                                                       AktørIdEntitet brukerAktørId,
                                                       OrganisasjonsnummerDto orgnr,
                                                       SaksnummerDto fagsakSaksnummer) {
        return forespørselRepository.finnSakUnderBehandling(brukerAktørId, ytelseType, orgnr.orgnr(), fagsakSaksnummer.saksnr());
    }

    public SakEntitet opprettSak(Ytelsetype ytelseType,
                                 AktørIdEntitet brukerAktørId,
                                 OrganisasjonsnummerDto orgnr,
                                 SaksnummerDto fagsakSaksnummer) {

        return forespørselRepository.opprettSak(ytelseType,
            brukerAktørId.getAktørId(),
            orgnr.orgnr(),
            fagsakSaksnummer.saksnr());
    }


    public UUID opprettForespørsel(LocalDate skjæringstidspunkt,
                                   Ytelsetype ytelseType,
                                   AktørIdEntitet brukerAktørId,
                                   OrganisasjonsnummerDto orgnr,
                                   SaksnummerDto fagsakSaksnummer) {

        return forespørselRepository.lagreForespørsel(skjæringstidspunkt, ytelseType, brukerAktørId.getAktørId(), orgnr.orgnr(),
            fagsakSaksnummer.saksnr());
    }

    public void setOppgaveId(UUID forespørselUUID, String oppgaveId) {
        forespørselRepository.oppdaterOppgaveId(forespørselUUID, oppgaveId);
    }

    public void setFagerSakId(Long internSakId, String fagerSakId) {
        forespørselRepository.oppdaterSakId(internSakId, fagerSakId);
    }


    public void ferdigstillSak(Long internSakId) {
        forespørselRepository.ferdigstillSak(internSakId);
    }

    public void utførForespørsel(UUID uuid) {
        forespørselRepository.utførForespørsel(uuid);
    }

    public Optional<ForespørselEntitet> finnÅpenForespørsel(LocalDate skjæringstidspunkt,
                                                            Ytelsetype ytelseType,
                                                            AktørIdEntitet brukerAktørId,
                                                            OrganisasjonsnummerDto orgnr, String fagsakSaksnummer) {
        return forespørselRepository.finnÅpenForespørsel(brukerAktørId, ytelseType, orgnr.orgnr(), fagsakSaksnummer, skjæringstidspunkt);
    }

    public Optional<ForespørselEntitet> finnForespørsel(UUID forespørselUuid) {
        return forespørselRepository.hentForespørsel(forespørselUuid);
    }

}
