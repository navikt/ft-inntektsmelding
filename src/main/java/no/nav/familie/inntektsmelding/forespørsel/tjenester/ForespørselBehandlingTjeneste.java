package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.rest.ForespørselDto;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public interface ForespørselBehandlingTjeneste {

    ForespørselResultat håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                                      Ytelsetype ytelsetype,
                                                      AktørIdEntitet aktørId,
                                                      OrganisasjonsnummerDto organisasjonsnummer,
                                                      SaksnummerDto fagsakSaksnummer,
                                                      LocalDate førsteUttaksdato);

    ForespørselEntitet ferdigstillForespørsel(UUID foresporselUuid,
                                AktørIdEntitet aktorId,
                                OrganisasjonsnummerDto organisasjonsnummerDto,
                                LocalDate startdato,
                                LukkeÅrsak årsak);

    Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID);

    List<ForespørselEntitet> hentForespørslerForFagsak(SaksnummerDto fagsakSaksnummer,
                                                       OrganisasjonsnummerDto orgnummerDto,
                                                       LocalDate skjæringstidspunkt);

    void oppdaterForespørsler(Ytelsetype ytelsetype,
                              AktørIdEntitet aktørId,
                              List<ForespørselDto> forespørsler,
                              SaksnummerDto fagsakSaksnummer);

    void opprettForespørsel(Ytelsetype ytelsetype,
                            AktørIdEntitet aktørId,
                            SaksnummerDto fagsakSaksnummer,
                            OrganisasjonsnummerDto organisasjonsnummer,
                            LocalDate skjæringstidspunkt,
                            LocalDate førsteUttaksdato,
                            String tilleggsinfo);

    void lukkForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt);

    void slettForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt);


    void settForespørselTilUtgått(SaksnummerDto saksnummerDto, OrganisasjonsnummerDto orgnummer, LocalDate skjæringstidspunkt);

    void settForespørselTilUtgått(ForespørselEntitet eksisterendeForespørsel, boolean skalOppdatereArbeidsgiverNotifikasjon);

    void gjenåpneForespørsel(ForespørselEntitet eksisterendeForespørsel);

    void opprettNyBeskjedMedEksternVarsling(SaksnummerDto fagsakSaksnummer,
                                            OrganisasjonsnummerDto organisasjonsnummer);
}
