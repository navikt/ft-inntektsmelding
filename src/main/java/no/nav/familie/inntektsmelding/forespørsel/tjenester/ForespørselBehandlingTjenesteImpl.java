package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
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
        var åpenForespørsel = forespørselTjeneste.finnÅpenForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer);

        if (åpenForespørsel.isPresent()) {
            var msg = String.format("Finnes allerede forespørsel for aktør %s på startdato %s + på ytelse %s", aktørId, skjæringstidspunkt,
                ytelsetype);
            LOG.info(msg);
            return;
        }

        opprettForespørselOppgave(ytelsetype, aktørId, fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt);
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

        arbeidsgiverNotifikasjon.oppgaveUtført(foresporsel.getOppgaveId(), OffsetDateTime.now());
        arbeidsgiverNotifikasjon.ferdigstillSak(foresporsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.STATUS_TEKST_DEFAULT); // Oppdaterer status i arbeidsgiver-notifikasjon
        forespørselTjeneste.ferdigstillForespørsel(foresporsel.getArbeidsgiverNotifikasjonSakId()); // Oppdaterer status i forespørsel
    }

    @Override
    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.finnForespørsel(forespørselUUID);
    }

    @Override
    public void oppdaterForespørsler(Ytelsetype ytelsetype,
                                     AktørIdEntitet aktørId,
                                     Map<LocalDate, List<OrganisasjonsnummerDto>> organisasjonerPerSkjæringstidspunkt,
                                     SaksnummerDto fagsakSaksnummer) {
        List<ForespørselEntitet> eksisterendeForespørsler = forespørselTjeneste.finnForespørslerForSak(fagsakSaksnummer);

        // Oppretter forespørsler for alle skjæringstidspunkter som ikke allerede er opprettet
        organisasjonerPerSkjæringstidspunkt.forEach((skjæringstidspunkt, organisasjoner) -> {
            organisasjoner.forEach(organisasjon -> {
                Optional<ForespørselEntitet> eksisterendeForespørsel = eksisterendeForespørsler.stream()
                    .filter(forespørsel -> forespørsel.getSkjæringstidspunkt().equals(skjæringstidspunkt))
                    .filter(forespørsel -> forespørsel.getOrganisasjonsnummer().equals(organisasjon.orgnr()))
                    .findFirst();

                if (eksisterendeForespørsel.isEmpty()) {
                    opprettForespørselOppgave(ytelsetype, aktørId, fagsakSaksnummer, organisasjon, skjæringstidspunkt);
                    var msg = String.format("Oppretter forespørsel, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
                        organisasjon.orgnr(), skjæringstidspunkt, fagsakSaksnummer.saksnr(), ytelsetype);
                    LOG.info(msg);
                }
            });
        });

        // Forespørsler som ikke lenger er aktuelle settes til utgått
        eksisterendeForespørsler.forEach(eksisterendeForespørsel -> {
            boolean trengerEksisterendeForespørsel = innholderRequestEksisterendeForespørsel(organisasjonerPerSkjæringstidspunkt,
                eksisterendeForespørsel);

            if (!trengerEksisterendeForespørsel && eksisterendeForespørsel.getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
                setForespørselTilUtgått(eksisterendeForespørsel);
            }
        });
    }

    private boolean innholderRequestEksisterendeForespørsel(Map<LocalDate, List<OrganisasjonsnummerDto>> organisasjonerPerSkjæringstidspunkt,
                                                            ForespørselEntitet eksisterendeForespørsel) {
        LocalDate stp = eksisterendeForespørsel.getSkjæringstidspunkt();
        List<String> orgnrFraRequestForStp = organisasjonerPerSkjæringstidspunkt.get(stp).stream().map(OrganisasjonsnummerDto::orgnr).toList();

        return orgnrFraRequestForStp.contains(eksisterendeForespørsel.getOrganisasjonsnummer());
    }

    private void opprettForespørselOppgave(Ytelsetype ytelsetype,
                                           AktørIdEntitet aktørId,
                                           SaksnummerDto fagsakSaksnummer,
                                           OrganisasjonsnummerDto organisasjonsnummer,
                                           LocalDate skjæringstidspunkt) {
        var uuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer);
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var arbeidsgiverNotifikasjonSakId = arbeidsgiverNotifikasjon.opprettSak(uuid.toString(),
            merkelapp,
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri,
            ForespørselTekster.STATUS_TEKST_DEFAULT);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(uuid, arbeidsgiverNotifikasjonSakId);

        var oppgaveId = arbeidsgiverNotifikasjon.opprettOppgave(uuid.toString(), merkelapp, uuid.toString(), organisasjonsnummer.orgnr(),
            ForespørselTekster.lagOppgaveTekst(person.mapFulltNavn()), skjemaUri);

        forespørselTjeneste.setOppgaveId(uuid, oppgaveId);
    }

    private void setForespørselTilUtgått(ForespørselEntitet eksisterendeForespørsel) {
        arbeidsgiverNotifikasjon.oppgaveUtgått(eksisterendeForespørsel.getOppgaveId(), OffsetDateTime.now());
        arbeidsgiverNotifikasjon.ferdigstillSak(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.STATUS_TEKST_DEFAULT); // Oppdaterer status i arbeidsgiver-notifikasjon
        forespørselTjeneste.settForespørselTilUtgått(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId());

        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            eksisterendeForespørsel.getOrganisasjonsnummer(),
            eksisterendeForespørsel.getSkjæringstidspunkt(),
            eksisterendeForespørsel.getFagsystemSaksnummer(),
            eksisterendeForespørsel.getYtelseType());
        LOG.info(msg);
    }

    public void lukkForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = forespørselTjeneste.finnÅpneForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt()))
            .toList();

        // Alle inntektsmeldinger sendt inn via arbeidsgiverportal blir lukket umiddelbart etter innsending fra #InntektsmeldingTjeneste,
        // så forespørsler som enda er åpne her blir løst ved innsending fra andre systemer
        forespørsler.forEach(f -> {
            MetrikkerTjeneste.loggForespørselLukkEkstern(f.getYtelseType());
            ferdigstillForespørsel(f.getUuid(), f.getAktørId(), new OrganisasjonsnummerDto(f.getOrganisasjonsnummer()), f.getSkjæringstidspunkt());
        });
    }

    public void lukkÅpneForespørsler(SaksnummerDto fagsakSaksnummer) {
        var forespørsler = forespørselTjeneste.finnÅpneForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> f.getStatus() == ForespørselStatus.UNDER_BEHANDLING)
            .toList();

        forespørsler.forEach(f -> {
            // Ønsker vi å legg til en metrikk her?
            setForespørselTilUtgått(f);
        });
    }

    private void validerStartdato(ForespørselEntitet forespørsel, LocalDate startdato) {
        if (!forespørsel.getSkjæringstidspunkt().equals(startdato)) {
            throw new IllegalStateException("Startdato var ikke like");
        }
    }

    private void validerOrganisasjon(ForespørselEntitet forespørsel, OrganisasjonsnummerDto orgnummer) {
        if (!forespørsel.getOrganisasjonsnummer().equals(orgnummer.orgnr())) {
            throw new IllegalStateException("Organisasjonsnummer var ikke like");
        }
    }

    private void validerAktør(ForespørselEntitet forespørsel, AktørIdEntitet aktorId) {
        if (!forespørsel.getAktørId().equals(aktorId)) {
            throw new IllegalStateException("AktørId for bruker var ikke like");
        }
    }
}
