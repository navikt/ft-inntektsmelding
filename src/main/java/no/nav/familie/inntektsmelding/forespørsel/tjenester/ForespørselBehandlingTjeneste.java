package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public interface ForespørselBehandlingTjeneste {

    void håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                       Ytelsetype ytelsetype,
                                       AktørIdEntitet aktørId,
                                       OrganisasjonsnummerDto organisasjonsnummer,
                                       SaksnummerDto fagsakSaksnummer);

    void ferdigstillForespørsel(UUID foresporselUuid, AktørIdEntitet aktorId, OrganisasjonsnummerDto organisasjonsnummerDto, LocalDate startdato);

    void ferdigstillSak(AktørIdEntitet aktørId, OrganisasjonsnummerDto organisasjonsnummerDto, Ytelsetype ytelsetype, SaksnummerDto fagsakSaksnummer);

    Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID);
}
