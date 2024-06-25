package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
class ForespørselBehandlingTjenesteImpl implements ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjenesteImpl.class);

    private static final no.nav.foreldrepenger.konfig.Environment ENV = Environment.current();

    private ForespørselTjeneste forespørselTjeneste;
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private PersonTjeneste personTjeneste;
    private String inntektsmeldingSkjemaLenke;

    public ForespørselBehandlingTjenesteImpl() {
    }

    @Inject
    public ForespørselBehandlingTjenesteImpl(ForespørselTjeneste forespørselTjeneste,
                                             ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon,
                                             PersonTjeneste personTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.personTjeneste = personTjeneste;
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke", "https://arbeidsgiver.intern.dev.nav.no/fp-im-dialog");
    }

    @Override
    public void håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                              Ytelsetype ytelsetype,
                                              AktørIdEntitet aktørId,
                                              OrganisasjonsnummerDto organisasjonsnummer,
                                              SaksnummerDto fagsakSaksnummer) {
        var åpenForespørsel = forespørselTjeneste.finnÅpenForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer,
            fagsakSaksnummer.saksnr());

        if (åpenForespørsel.isPresent()) {
            var msg = String.format("Finnes allerede forespørsel for aktør %s på startdato %s + på ytelse %s", aktørId, skjæringstidspunkt,
                ytelsetype);
            LOG.info(msg);
            return;
        }

        opprettSakOmIngenÅpen(ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer);
        var uuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer);
        var merkelapp = finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var oppgaveId = arbeidsgiverNotifikasjon.opprettOppgave(uuid.toString(), merkelapp, uuid.toString(), organisasjonsnummer.orgnr(),
            "NAV trenger inntektsmelding for " + finnYtelseNavn(ytelsetype), skjemaUri);
        forespørselTjeneste.setOppgaveId(uuid, oppgaveId);

    }

    private String finnYtelseNavn(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case OMSORGSPENGER -> "omsorgspenger";
            case FORELDREPENGER -> "foreldrepenger";
            case PLEIEPENGER_SYKT_BARN -> "pleiepenger sykt barn";
            case SVANGERSKAPSPENGER -> "svangerskapspenger";
            case OPPLÆRINGSPENGER -> "opplæringspenger";
            case PLEIEPENGER_NÆRSTÅENDE -> "pleiepenger i livets sluttfase";
        };
    }

    private void opprettSakOmIngenÅpen(Ytelsetype ytelsetype,
                                       AktørIdEntitet aktørId,
                                       OrganisasjonsnummerDto organisasjonsnummer,
                                       SaksnummerDto fagsakSaksnummer) {
        var sak = forespørselTjeneste.finnSakUnderBehandling(ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer);
        if (sak.isEmpty()) {
            var person = personTjeneste.hentPersonInfo(aktørId, ytelsetype);
            var merkelapp = finnMerkelapp(ytelsetype);
            var sakEntitet = forespørselTjeneste.opprettSak(ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer);
            var sakId = arbeidsgiverNotifikasjon.opprettSak(sakEntitet.getGrupperingUuid().toString(), merkelapp, organisasjonsnummer.orgnr(),
                lagSaksTittel(person), null);
            forespørselTjeneste.setFagerSakId(sakEntitet.getId(), sakId);
        }
    }

    @Override
    public void ferdigstillForespørsel(UUID foresporselUuid,
                                       AktørIdEntitet aktorId,
                                       OrganisasjonsnummerDto organisasjonsnummerDto,
                                       LocalDate startdato) {
        var foresporsel = forespørselTjeneste.finnForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));
        validerAktør(foresporsel, aktorId);
        validerOrganisasjon(foresporsel, organisasjonsnummerDto);
        validerStartdato(foresporsel, startdato);

        arbeidsgiverNotifikasjon.lukkOppgave(foresporsel.getOppgaveId(), OffsetDateTime.now());
        forespørselTjeneste.utførForespørsel(foresporselUuid);

    }

    @Override
    public void ferdigstillSak(AktørIdEntitet aktørId,
                               OrganisasjonsnummerDto organisasjonsnummerDto,
                               Ytelsetype ytelsetype,
                               SaksnummerDto fagsakSaksnummer) {
        var sak = forespørselTjeneste.finnSakUnderBehandling(ytelsetype, aktørId, organisasjonsnummerDto, fagsakSaksnummer);
        if (sak.isEmpty()) {
            throw new IllegalStateException("Fant ingen sak for ferdigsilling");
        }
        arbeidsgiverNotifikasjon.ferdigstillSak(sak.get().getFagerSakId()); // Oppdaterer status i arbeidsgiver-notifikasjon
        forespørselTjeneste.ferdigstillSak(sak.get().getId()); // Oppdaterer vår status
    }

    @Override
    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.finnForespørsel(forespørselUUID);
    }

    private void validerStartdato(ForespørselEntitet forespørsel, LocalDate startdato) {
        if (!forespørsel.getSkjæringstidspunkt().equals(startdato)) {
            throw new IllegalStateException("Startdato var ikke like");
        }
    }

    private void validerOrganisasjon(ForespørselEntitet forespørsel, OrganisasjonsnummerDto orgnummer) {
        if (!forespørsel.getSak().getOrganisasjonsnummer().equals(orgnummer.orgnr())) {
            throw new IllegalStateException("Organisasjonsnummer var ikke like");
        }
    }

    private void validerAktør(ForespørselEntitet forespørsel, AktørIdEntitet aktorId) {
        if (!forespørsel.getSak().getAktørId().equals(aktorId)) {
            throw new IllegalStateException("AktørId for bruker var ikke like");
        }
    }

    private Merkelapp finnMerkelapp(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> Merkelapp.INNTEKTSMELDING_FP;
            case PLEIEPENGER_SYKT_BARN -> Merkelapp.INNTEKTSMELDING_PSB;
            case OMSORGSPENGER -> Merkelapp.INNTEKTSMELDING_OMP;
            case SVANGERSKAPSPENGER -> Merkelapp.INNTEKTSMELDING_SVP;
            case PLEIEPENGER_NÆRSTÅENDE -> Merkelapp.INNTEKTSMELDING_PILS;
            case OPPLÆRINGSPENGER -> Merkelapp.INNTEKTSMELDING_OPP;
        };
    }

    protected String lagSaksTittel(PersonInfo personInfo) {
        return String.format("Inntektsmelding for %s: f. %s", StringUtils.capitalize(personInfo.mapNavn()),
            personInfo.fødselsdato().format(DateTimeFormatter.ofPattern("ddMMyy")));
    }
}
