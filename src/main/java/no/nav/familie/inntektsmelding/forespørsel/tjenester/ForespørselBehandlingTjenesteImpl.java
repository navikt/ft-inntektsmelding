package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.typer.dto.NyBeskjedResultat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.rest.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.GjenåpneForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.OpprettForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.SettForespørselTilUtgåttTask;
import no.nav.familie.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
class ForespørselBehandlingTjenesteImpl implements ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjenesteImpl.class);

    private static final no.nav.foreldrepenger.konfig.Environment ENV = Environment.current();

    private final ForespørselTjeneste forespørselTjeneste;
    private final ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private final PersonTjeneste personTjeneste;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final OrganisasjonTjeneste organisasjonTjeneste;
    private final String inntektsmeldingSkjemaLenke;

    @Inject
    public ForespørselBehandlingTjenesteImpl(ForespørselTjeneste forespørselTjeneste,
                                             ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon,
                                             PersonTjeneste personTjeneste,
                                             ProsessTaskTjeneste prosessTaskTjeneste,
                                             OrganisasjonTjeneste organisasjonTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.personTjeneste = personTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke", "https://arbeidsgiver.intern.dev.nav.no/fp-im-dialog");
    }

    @Override
    public ForespørselEntitet ferdigstillForespørsel(UUID foresporselUuid,
                                                     AktørIdEntitet aktorId,
                                                     OrganisasjonsnummerDto organisasjonsnummerDto,
                                                     LocalDate startdato,
                                                     LukkeÅrsak årsak) {
        var foresporsel = forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));

        validerAktør(foresporsel, aktorId);
        validerOrganisasjon(foresporsel, organisasjonsnummerDto);
        validerStartdato(foresporsel, startdato);


        // Arbeidsgiverinitierte forespørsler har ingen oppgave
        foresporsel.getOppgaveId().map(oppgaveId -> arbeidsgiverNotifikasjon.oppgaveUtført(oppgaveId, OffsetDateTime.now()));

        var erArbeidsgiverInitiertInntektsmelding = foresporsel.getOppgaveId().isEmpty();
        arbeidsgiverNotifikasjon.ferdigstillSak(foresporsel.getArbeidsgiverNotifikasjonSakId(), erArbeidsgiverInitiertInntektsmelding); // Oppdaterer status i arbeidsgiver-notifikasjon
        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(foresporsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(årsak, foresporsel.getSkjæringstidspunkt()));
        forespørselTjeneste.ferdigstillForespørsel(foresporsel.getArbeidsgiverNotifikasjonSakId()); // Oppdaterer status i forespørsel
        return foresporsel;
    }

    @Override
    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.hentForespørsel(forespørselUUID);
    }

    @Override
    public List<ForespørselEntitet> finnForespørsler(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselTjeneste.finnForespørsler(aktørId, ytelsetype, orgnr);
    }

    @Override
    public void oppdaterForespørsler(Ytelsetype ytelsetype,
                                     AktørIdEntitet aktørId,
                                     List<OppdaterForespørselDto> forespørsler,
                                     SaksnummerDto fagsakSaksnummer) {
        final var eksisterendeForespørsler = forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer);
        final var taskGruppe = new ProsessTaskGruppe();

        // Forespørsler som skal opprettes
        var skalOpprettes = utledNyeForespørsler(forespørsler, eksisterendeForespørsler);
        for (OppdaterForespørselDto forespørselDto : skalOpprettes) {
            var opprettForespørselTask = OpprettForespørselTask.lagTaskData(ytelsetype,
                aktørId,
                fagsakSaksnummer,
                forespørselDto.orgnr(),
                forespørselDto.skjæringstidspunkt());
            taskGruppe.addNesteParallell(opprettForespørselTask);
        }

        // Forespørsler som skal settes til utgått
        var skalSettesUtgått = utledForespørslerSomSkalSettesUtgått(forespørsler, eksisterendeForespørsler);
        for (ForespørselEntitet forespørsel : skalSettesUtgått) {
            var settForespørselTilUtgåttTask = ProsessTaskData.forProsessTask(SettForespørselTilUtgåttTask.class);
            settForespørselTilUtgåttTask.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, forespørsel.getUuid().toString());
            settForespørselTilUtgåttTask.setSaksnummer(fagsakSaksnummer.saksnr());
            taskGruppe.addNesteParallell(settForespørselTilUtgåttTask);
        }

        // Forespørsler som skal gjenåpnes
        var skalGjenåpnes = utledForespørslerSomSkalGjenåpnes(forespørsler, eksisterendeForespørsler);
        for (ForespørselEntitet forespørsel : skalGjenåpnes) {
            var gjenåpneForespørselTask = ProsessTaskData.forProsessTask(GjenåpneForespørselTask.class);
            gjenåpneForespørselTask.setProperty(GjenåpneForespørselTask.FORESPØRSEL_UUID, forespørsel.getUuid().toString());
            gjenåpneForespørselTask.setSaksnummer(fagsakSaksnummer.saksnr());
            taskGruppe.addNesteParallell(gjenåpneForespørselTask);
        }

        if (!taskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(taskGruppe);
        } else {
            LOG.info("Ingen oppdatering er nødvendig for saksnr: {}", fagsakSaksnummer);
        }
    }

    private static List<OppdaterForespørselDto> utledNyeForespørsler(List<OppdaterForespørselDto> forespørsler,
                                                                     List<ForespørselEntitet> eksisterendeForespørsler) {
        // Skal opprette forespørsler for alle skjæringstidspunkt som ikke allerede er opprettet
        return forespørsler.stream()
            .filter(f -> f.aksjon() == ForespørselAksjon.OPPRETT)
            .filter(f -> finnEksisterendeForespørsel(f,
                eksisterendeForespørsler,
                List.of(ForespørselStatus.UNDER_BEHANDLING, ForespørselStatus.FERDIG)).isEmpty())
            .toList();
    }

    private static List<ForespørselEntitet> utledForespørslerSomSkalSettesUtgått(List<OppdaterForespørselDto> forespørsler,
                                                                                 List<ForespørselEntitet> eksisterendeForespørsler) {
        List<ForespørselEntitet> skalSettesUtgått = new ArrayList<>();
        // Forespørsler som ikke lenger er aktuelle settes til utgått
        for (ForespørselEntitet eksisterendeForespørsel : eksisterendeForespørsler) {
            if (eksisterendeForespørsel.getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
                boolean trengerEksisterendeForespørsel = innholderRequestEksisterendeForespørsel(forespørsler, eksisterendeForespørsel);
                if (!trengerEksisterendeForespørsel) {
                    skalSettesUtgått.add(eksisterendeForespørsel);
                }
            }
        }
        // Forespørsler som skal sperres for endringer settes til utgått
        for (OppdaterForespørselDto forespørselDto : forespørsler) {
            if (forespørselDto.aksjon() == ForespørselAksjon.UTGÅTT) {
                var skalSperresForEndringer = finnEksisterendeForespørsel(forespørselDto,
                    eksisterendeForespørsler,
                    List.of(ForespørselStatus.values()));
                skalSperresForEndringer.ifPresent(skalSettesUtgått::add);
            }
        }
        return skalSettesUtgått;
    }

    private static List<ForespørselEntitet> utledForespørslerSomSkalGjenåpnes(List<OppdaterForespørselDto> forespørsler,
                                                                              List<ForespørselEntitet> eksisterendeForespørsler) {
        List<ForespørselEntitet> forespørslerSomSkalGjenåpnes = new ArrayList<>();
        for (OppdaterForespørselDto forespørselDto : forespørsler) {
            if (forespørselDto.aksjon() == ForespørselAksjon.GJENOPPRETT) {
                var skalGjenåpnes = finnEksisterendeForespørsel(forespørselDto,
                    eksisterendeForespørsler,
                    List.of(ForespørselStatus.UTGÅTT));
                skalGjenåpnes.ifPresent(forespørslerSomSkalGjenåpnes::add);
            }
        }
        return forespørslerSomSkalGjenåpnes;
    }

    private static Optional<ForespørselEntitet> finnEksisterendeForespørsel(OppdaterForespørselDto forespørselDto,
                                                                            List<ForespørselEntitet> eksisterendeForespørsler,
                                                                            List<ForespørselStatus> statuser) {
        return eksisterendeForespørsler.stream()
            .filter(f -> f.getSkjæringstidspunkt().equals(forespørselDto.skjæringstidspunkt()))
            .filter(f -> f.getOrganisasjonsnummer().equals(forespørselDto.orgnr().orgnr()))
            .filter(f -> statuser.contains(f.getStatus()))
            .findFirst();
    }

    private static boolean innholderRequestEksisterendeForespørsel(List<OppdaterForespørselDto> forepørsler,
                                                                   ForespørselEntitet eksisterendeForespørsel) {
        return forepørsler.stream()
            .anyMatch(forespørselDto -> forespørselDto.orgnr().orgnr().equals(eksisterendeForespørsel.getOrganisasjonsnummer()) &&
                forespørselDto.skjæringstidspunkt().equals(eksisterendeForespørsel.getSkjæringstidspunkt()));
    }

    @Override
    public void settForespørselTilUtgått(ForespørselEntitet eksisterendeForespørsel, boolean skalOppdatereArbeidsgiverNotifikasjon) {
        if (skalOppdatereArbeidsgiverNotifikasjon) {
            eksisterendeForespørsel.getOppgaveId().map(oppgaveId -> arbeidsgiverNotifikasjon.oppgaveUtgått(oppgaveId, OffsetDateTime.now()));
            arbeidsgiverNotifikasjon.ferdigstillSak(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
                false); // Oppdaterer status i arbeidsgiver-notifikasjon
        }

        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, eksisterendeForespørsel.getSkjæringstidspunkt()));
        forespørselTjeneste.settForespørselTilUtgått(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId());

        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            eksisterendeForespørsel.getOrganisasjonsnummer(),
            eksisterendeForespørsel.getSkjæringstidspunkt(),
            eksisterendeForespørsel.getFagsystemSaksnummer(),
            eksisterendeForespørsel.getYtelseType());
        LOG.info(msg);
    }

    @Override
    public void gjenåpneForespørsel(ForespørselEntitet eksisterendeForespørsel) {
        if (eksisterendeForespørsel.getStatus() != ForespørselStatus.UTGÅTT) {
            throw new IllegalArgumentException(
                "Forespørsel som skal gjenåpnes må ha status UTGÅTT, var " + eksisterendeForespørsel.getStatus() + ". " + eksisterendeForespørsel);
        }
        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(), null);
        forespørselTjeneste.ferdigstillForespørsel(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId());

        var msg = String.format("Gjenåpner forespørsel, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            eksisterendeForespørsel.getOrganisasjonsnummer(),
            eksisterendeForespørsel.getSkjæringstidspunkt(),
            eksisterendeForespørsel.getFagsystemSaksnummer(),
            eksisterendeForespørsel.getYtelseType());
        LOG.info(msg);
    }

    public void opprettForespørsel(Ytelsetype ytelsetype,
                                   AktørIdEntitet aktørId,
                                   SaksnummerDto fagsakSaksnummer,
                                   OrganisasjonsnummerDto organisasjonsnummer,
                                   LocalDate skjæringstidspunkt,
                                   LocalDate førsteUttaksdato) {
        var msg = String.format("Oppretter forespørsel, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            organisasjonsnummer,
            skjæringstidspunkt,
            fagsakSaksnummer.saksnr(),
            ytelsetype);
        LOG.info(msg);

        var organisasjon = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr());

        var uuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            organisasjonsnummer,
            fagsakSaksnummer,
            førsteUttaksdato);
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var arbeidsgiverNotifikasjonSakId = arbeidsgiverNotifikasjon.opprettSak(uuid.toString(),
            merkelapp,
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(arbeidsgiverNotifikasjonSakId, ForespørselTekster.lagTilleggsInformasjonOrdinær(skjæringstidspunkt));

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(uuid, arbeidsgiverNotifikasjonSakId);

        String oppgaveId;
        try {
            oppgaveId = arbeidsgiverNotifikasjon.opprettOppgave(uuid.toString(),
                merkelapp,
                uuid.toString(),
                organisasjonsnummer.orgnr(),
                ForespørselTekster.lagOppgaveTekst(ytelsetype),
                ForespørselTekster.lagVarselTekst(ytelsetype, organisasjon),
                ForespørselTekster.lagPåminnelseTekst(ytelsetype, organisasjon),
                skjemaUri);
        } catch (Exception e) {
            //Manuell rollback er nødvendig fordi sak og oppgave går i to forskjellige kall
            arbeidsgiverNotifikasjon.slettSak(arbeidsgiverNotifikasjonSakId);
            throw e;
        }

        forespørselTjeneste.setOppgaveId(uuid, oppgaveId);
    }

    public UUID opprettForespørselForArbeidsgiverInitiertIm(Ytelsetype ytelsetype,
                                                            AktørIdEntitet aktørId,
                                                            SaksnummerDto fagsakSaksnummer,
                                                            OrganisasjonsnummerDto organisasjonsnummer,
                                                            LocalDate skjæringstidspunkt,
                                                            LocalDate førsteUttaksdato) {
        var msg = String.format("Oppretter forespørsel for arbeidsgiverinitiert, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            organisasjonsnummer,
            skjæringstidspunkt,
            fagsakSaksnummer.saksnr(),
            ytelsetype);
        LOG.info(msg);

        var uuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            organisasjonsnummer,
            fagsakSaksnummer,
            førsteUttaksdato);

        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var fagerSakId = arbeidsgiverNotifikasjon.opprettSak(uuid.toString(),
            merkelapp,
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(uuid, fagerSakId);

        return uuid;
    }

    @Override
    public NyBeskjedResultat opprettNyBeskjedMedEksternVarsling(SaksnummerDto fagsakSaksnummer,
                                                                OrganisasjonsnummerDto organisasjonsnummer) {
        var forespørsel = forespørselTjeneste.finnÅpenForespørslelForFagsak(fagsakSaksnummer, organisasjonsnummer)
            .orElse(null);

        if (forespørsel == null) {
            return NyBeskjedResultat.FORESPØRSEL_FINNES_IKKE;
        }

        var msg = String.format("Oppretter ny beskjed med ekstern varsling, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            organisasjonsnummer,
            forespørsel.getSkjæringstidspunkt(),
            fagsakSaksnummer.saksnr(),
            forespørsel.getYtelseType());
        LOG.info(msg);
        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.getYtelseType());
        var forespørselUuid = forespørsel.getUuid();
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørselUuid);
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr());
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType());
        var varselTekst = ForespørselTekster.lagVarselFraSaksbehandlerTekst(forespørsel.getYtelseType(), organisasjon);
        var beskjedTekst = ForespørselTekster.lagBeskjedFraSaksbehandlerTekst(forespørsel.getYtelseType(), person.mapFulltNavn());

        arbeidsgiverNotifikasjon.opprettNyBeskjedMedEksternVarsling(forespørselUuid.toString(),
            merkelapp,
            forespørselUuid.toString(),
            organisasjonsnummer.orgnr(),
            beskjedTekst,
            varselTekst,
            skjemaUri);

        return NyBeskjedResultat.NY_BESKJED_SENDT;
    }

    @Override
    public void lukkForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, orgnummerDto, skjæringstidspunkt);

        // Alle inntektsmeldinger sendt inn via arbeidsgiverportal blir lukket umiddelbart etter innsending fra #InntektsmeldingTjeneste,
        // så forespørsler som enda er åpne her blir løst ved innsending fra andre systemer
        forespørsler.forEach(f -> {
            var lukketForespørsel = ferdigstillForespørsel(f.getUuid(),
                f.getAktørId(),
                new OrganisasjonsnummerDto(f.getOrganisasjonsnummer()),
                f.getFørsteUttaksdato().orElseGet(f::getSkjæringstidspunkt),
                LukkeÅrsak.EKSTERN_INNSENDING);
            MetrikkerTjeneste.loggForespørselLukkEkstern(lukketForespørsel);
        });
    }

    @Override
    public void settForespørselTilUtgått(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, orgnummerDto, skjæringstidspunkt);

        forespørsler.forEach(it -> settForespørselTilUtgått(it, true));
    }

    private List<ForespørselEntitet> hentÅpneForespørslerForFagsak(SaksnummerDto fagsakSaksnummer,
                                                                   OrganisasjonsnummerDto orgnummerDto,
                                                                   LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnÅpneForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> orgnummerDto == null || orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt()))
            .toList();
    }

    @Override
    public List<ForespørselEntitet> hentForespørslerForFagsak(SaksnummerDto fagsakSaksnummer,
                                                              OrganisasjonsnummerDto orgnummerDto,
                                                              LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> orgnummerDto == null || orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt()))
            .toList();
    }

    @Override
    public void slettForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var sakerSomSkalSlettes = forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> skjæringstidspunkt == null || f.getSkjæringstidspunkt().equals(skjæringstidspunkt))
            .filter(f -> orgnummerDto == null || f.getOrganisasjonsnummer().equals(orgnummerDto.orgnr()))
            .filter(f -> f.getStatus().equals(ForespørselStatus.UNDER_BEHANDLING))
            .toList();

        if (sakerSomSkalSlettes.size() != 1) {
            var msg = String.format("Fant ikke akkurat 1 sak som skulle slettes. Fant istedet %s saker ", sakerSomSkalSlettes.size());
            throw new IllegalStateException(msg);
        }
        var agPortalSakId = sakerSomSkalSlettes.getFirst().getArbeidsgiverNotifikasjonSakId();
        arbeidsgiverNotifikasjon.slettSak(agPortalSakId);
        forespørselTjeneste.settForespørselTilUtgått(agPortalSakId);
    }

    @Override
    public List<InntektsmeldingForespørselDto> finnForespørslerForFagsak(SaksnummerDto fagsakSaksnummer) {
        return forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream().map(forespoersel ->
                new InntektsmeldingForespørselDto(
                    forespoersel.getUuid(),
                    forespoersel.getSkjæringstidspunkt(),
                    forespoersel.getOrganisasjonsnummer(),
                    forespoersel.getAktørId().getAktørId(),
                    forespoersel.getYtelseType().toString(),
                    forespoersel.getFørsteUttaksdato().orElse(null)))
            .toList();
    }

    @Override
    public List<ForespørselEntitet> finnForespørslerForAktørId(AktørIdEntitet aktørIdEntitet, Ytelsetype ytelsetype) {
        return forespørselTjeneste.finnForespørslerForAktørid(aktørIdEntitet, ytelsetype);
    }

    @Override
    public Optional<ForespørselEntitet> finnOpprinneligForespørsel(AktørIdEntitet aktørId, Ytelsetype ytelseType, LocalDate startdato) {
        return finnForespørslerForAktørId(aktørId, ytelseType).stream()
            .filter(f -> f.getFørsteUttaksdato().orElse(f.getSkjæringstidspunkt()).isBefore(startdato))
            .max(Comparator.comparing(f -> f.getFørsteUttaksdato().orElse(f.getSkjæringstidspunkt())));
    }

    private void validerStartdato(ForespørselEntitet forespørsel, LocalDate startdato) {
        var datoÅMatcheMot = forespørsel.getFørsteUttaksdato().orElseGet(forespørsel::getSkjæringstidspunkt);
        if (!datoÅMatcheMot.equals(startdato)) {
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
