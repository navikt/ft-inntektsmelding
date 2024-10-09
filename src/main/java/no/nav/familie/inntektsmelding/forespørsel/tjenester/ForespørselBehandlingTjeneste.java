package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID);

    void oppdaterForespørsler(Ytelsetype ytelsetype,
                              AktørIdEntitet aktørId,
                              Map<LocalDate, List<OrganisasjonsnummerDto>> organisasjonerPerSkjæringstidspunkt,
                              SaksnummerDto fagsakSaksnummer);

    void lukkForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt);

    void lukkÅpneForespørsler(SaksnummerDto fagsakSaksnummer);
}
