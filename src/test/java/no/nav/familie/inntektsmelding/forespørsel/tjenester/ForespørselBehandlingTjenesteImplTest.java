package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.forespørsel.rest.ForespørselDto;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.OpprettForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.SettForespørselTilUtgåttTask;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith({JpaExtension.class, MockitoExtension.class})
public class ForespørselBehandlingTjenesteImplTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String AKTØR_ID = "1234567891234";
    private static final String SAK_ID = "1";
    private static final String OPPGAVE_ID = "2";
    private static final String SAK_ID_2 = "3";
    private static final String OPPGAVE_ID_2 = "4";
    private static final String SAKSNUMMMER = "FAGSAK_SAKEN";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusYears(1);
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.now().minusYears(1).plusDays(1);
    private static final Ytelsetype YTELSETYPE = Ytelsetype.PLEIEPENGER_SYKT_BARN;

    @Mock
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;

    private ForespørselRepository forespørselRepository;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        this.forespørselBehandlingTjeneste = new ForespørselBehandlingTjenesteImpl(new ForespørselTjeneste(forespørselRepository),
            arbeidsgiverNotifikasjon,
            personTjeneste,
            prosessTaskTjeneste,
            organisasjonTjeneste);
    }

    @Test
    void skal_opprette_forespørsel_og_sette_sak_og_oppgave() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("test org", BRREG_ORGNUMMER));

        var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            SKJÆRINGSTIDSPUNKT);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));

        assertThat(resultat).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
        assertThat(lagret.size()).isEqualTo(1);
        assertThat(lagret.getFirst().getArbeidsgiverNotifikasjonSakId()).isEqualTo(SAK_ID);
        assertThat(lagret.getFirst().getOppgaveId()).isEqualTo(OPPGAVE_ID);
    }

    @Test
    void eksisterende_forespørsel_på_samme_stp_skal_gi_nei() {
        forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER, SKJÆRINGSTIDSPUNKT);

        getEntityManager().clear();

        var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            SKJÆRINGSTIDSPUNKT);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));
        assertThat(resultat).isEqualTo(ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE);
        assertThat(lagret.size()).isEqualTo(1);
    }

    @Test
    void skal_ikke_opprette_forespørsel_når_finnes_allerede_for_stp_og_første_uttaksdato() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("test org", BRREG_ORGNUMMER));

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER)).getFirst();
        var fpEntitet = forespørselBehandlingTjeneste.ferdigstillForespørsel(lagret.getUuid(),
            lagret.getAktørId(),
            new OrganisasjonsnummerDto(lagret.getOrganisasjonsnummer()),
            lagret.getFørsteUttaksdato().orElse(lagret.getSkjæringstidspunkt()),
            LukkeÅrsak.EKSTERN_INNSENDING);

        assertThat(fpEntitet.getStatus()).isEqualTo(ForespørselStatus.FERDIG);

        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO);

        assertThat(resultat2).isEqualTo(ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE);
    }

    @Test
    void skal_opprette_forespørsel_når_finnes_allerede_for_samme_stp_og_ulik_uttaksdato() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("test org", BRREG_ORGNUMMER));

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER)).getFirst();

        var fpEntitet = forespørselBehandlingTjeneste.ferdigstillForespørsel(lagret.getUuid(),
            lagret.getAktørId(),
            new OrganisasjonsnummerDto(lagret.getOrganisasjonsnummer()),
            lagret.getFørsteUttaksdato().orElse(lagret.getSkjæringstidspunkt()),
            LukkeÅrsak.EKSTERN_INNSENDING);

        assertThat(fpEntitet.getStatus()).isEqualTo(ForespørselStatus.FERDIG);

        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO.plusDays(1));

        assertThat(resultat2).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
    }

    @Test
    void skal_ferdigstille_forespørsel() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT,
            LukkeÅrsak.EKSTERN_INNSENDING);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.FERDIG);
    }

    @Test
    void skal_ferdigstille_forespørsel_ulik_stp_og_startdato() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            FØRSTE_UTTAKSDATO,
            LukkeÅrsak.EKSTERN_INNSENDING);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.FERDIG);
    }

    @Test
    void skal_sette_alle_forespørspørsler_for_sak_til_ferdig() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            FØRSTE_UTTAKSDATO.plusDays(1));
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.FERDIG);
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.get().getStatus()).isEqualTo(ForespørselStatus.FERDIG);
    }

    @Test
    void skal_sette_alle_forespørspørsler_for_sak_til_utgått() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            FØRSTE_UTTAKSDATO);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.settForespørselTilUtgått(new SaksnummerDto(SAKSNUMMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.get().getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);
    }

    @Test
    void skal_lukke_forespørsel_for_sak_med_gitt_stp() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(new SaksnummerDto(SAKSNUMMMER),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.FERDIG);
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.get().getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
    }

    @Test
    void skal_opprette_forespørsel_dersom_det_ikke_eksisterer_en_for_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørsler = List.of(new ForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), false));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks().size()).isEqualTo(1);
        var taskdata = taskGruppe.getTasks().getFirst().task();
        assertThat(taskdata.taskType()).isEqualTo(TaskType.forProsessTask(OpprettForespørselTask.class));
        assertThat(taskdata.getPropertyValue(OpprettForespørselTask.YTELSETYPE)).isEqualTo(YTELSETYPE.toString());
        assertThat(taskdata.getSaksnummer()).isEqualTo(SAKSNUMMMER);
        assertThat(taskdata.getAktørId()).isEqualTo(AKTØR_ID);
        assertThat(taskdata.getPropertyValue(OpprettForespørselTask.ORGNR)).isEqualTo(BRREG_ORGNUMMER);
        assertThat(taskdata.getPropertyValue(OpprettForespørselTask.STP)).isEqualTo(SKJÆRINGSTIDSPUNKT.toString());
    }

    @Test
    void skal_ikke_opprette_ny_forespørsel_dersom_det_eksisterer_en_for_samme_stp() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørsler = List.of(new ForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), false));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        verifyNoInteractions(prosessTaskTjeneste);
    }

    @Test
    void skal_opprette_ny_forespørsel_og_beholde_gammel_dersom_vi_ber_om_et_nytt_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørsler = List.of(new ForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), false),
            new ForespørselDto(SKJÆRINGSTIDSPUNKT.plusDays(10), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), false));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks().size()).isEqualTo(1);
        var taskdata1 = taskGruppe.getTasks().getFirst().task();
        assertThat(taskdata1.taskType()).isEqualTo(TaskType.forProsessTask(OpprettForespørselTask.class));
    }

    @Test
    void skal_opprette_ny_forespørsel_og_markere_gammel_som_utgått_dersom_vi_erstatter_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørsler = List.of(new ForespørselDto(SKJÆRINGSTIDSPUNKT.plusDays(10), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), false));

        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID_2, OPPGAVE_ID_2);
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks().size()).isEqualTo(2);
        var taskdata1 = taskGruppe.getTasks().get(0).task();
        assertThat(taskdata1.taskType()).isEqualTo(TaskType.forProsessTask(OpprettForespørselTask.class));
        var taskdata2 = taskGruppe.getTasks().get(1).task();
        assertThat(taskdata2.taskType()).isEqualTo(TaskType.forProsessTask(SettForespørselTilUtgåttTask.class));
        assertThat(taskdata2.getPropertyValue(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID)).isEqualTo(forespørselUuid.toString());
    }

    @Test
    void skal_sperre_forespørsel_for_endringer() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        var forespørsler = List.of(new ForespørselDto(SKJÆRINGSTIDSPUNKT, new OrganisasjonsnummerDto(BRREG_ORGNUMMER), true));
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), forespørsler, new SaksnummerDto(SAKSNUMMMER));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var taskGruppe = captor.getValue();
        assertThat(taskGruppe.getTasks().size()).isEqualTo(1);
        var taskdata = taskGruppe.getTasks().getFirst().task();
        assertThat(taskdata.taskType()).isEqualTo(TaskType.forProsessTask(SettForespørselTilUtgåttTask.class));
        assertThat(taskdata.getPropertyValue(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID)).isEqualTo(forespørselUuid.toString());
    }

    @Test
    void skal_slette_oppgave_gitt_saksnummer_og_orgnr() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        when(arbeidsgiverNotifikasjon.slettSak(SAK_ID)).thenReturn(SAK_ID);

        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);
        verify(arbeidsgiverNotifikasjon, Mockito.times(1)).slettSak(SAK_ID);
    }

    @Test
    void skal_slette_oppgave_gitt_saksnummer() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        when(arbeidsgiverNotifikasjon.slettSak(SAK_ID)).thenReturn(SAK_ID);

        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(SAKSNUMMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);
        verify(arbeidsgiverNotifikasjon, Mockito.times(1)).slettSak(SAK_ID);
    }

    @Test
    void skal_opprette_ny_beskjed() {
        String varseltekst = "TEST A/S - orgnr 974760673: Vi har ennå ikke mottatt inntektsmelding. For at vi skal kunne behandle søknaden om foreldrepenger, må inntektsmeldingen sendes inn så raskt som mulig.";
        String beskjedtekst = "Vi har ennå ikke mottatt inntektsmelding for Navn Navnesen. For at vi skal kunne behandle søknaden om foreldrepenger, må inntektsmeldingen sendes inn så raskt som mulig.";
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, Ytelsetype.FORELDREPENGER, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var uri = URI.create(String.format("https://arbeidsgiver.intern.dev.nav.no/fp-im-dialog/%s", forespørselUuid.toString()));

        var personInfo = new PersonInfo("Navn",
            null,
            "Navnesen",
            new PersonIdent("01019100000"),
            new AktørIdEntitet(AKTØR_ID),
            LocalDate.of(1991, 1, 1).minusYears(30),
            null);

        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("Test A/S", BRREG_ORGNUMMER));
        when(arbeidsgiverNotifikasjon.opprettNyBeskjedMedEksternVarsling(forespørselUuid.toString(), Merkelapp.INNTEKTSMELDING_FP, forespørselUuid.toString(), BRREG_ORGNUMMER, beskjedtekst, varseltekst,
            uri)).thenReturn("beskjedId");
        when(personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(AKTØR_ID), Ytelsetype.FORELDREPENGER)).thenReturn(personInfo);

        forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER));

        clearHibernateCache();

        verify(arbeidsgiverNotifikasjon, Mockito.times(1)).opprettNyBeskjedMedEksternVarsling(forespørselUuid.toString(), Merkelapp.INNTEKTSMELDING_FP, forespørselUuid.toString(), BRREG_ORGNUMMER, beskjedtekst, varseltekst,
            uri);
    }

    @Test
    void skal_feile_ved_opprettelse_av_beskjed_om_det_ikke_finnes_åpen_forespørsel() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, Ytelsetype.FORELDREPENGER, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        assertThrows(IllegalStateException.class, () -> forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER)));
    }

    private void clearHibernateCache() {
        // Fjerne hibernate cachen før assertions skal evalueres - hibernate ignorerer alle updates som er markert med updatable = false ved skriving mot databasen
        // men objektene i cachen blir oppdatert helt greit likevel.
        // På denne måten evaluerer vi faktisk tilstanden som blir til slutt lagret i databasen.
        getEntityManager().clear();
    }

    private void mockInfoForOpprettelse(String aktørId, Ytelsetype ytelsetype, String brregOrgnummer, String sakId, String oppgaveId) {
        var personInfo = new PersonInfo("Navn",
            null,
            "Navnesen",
            new PersonIdent("01019100000"),
            new AktørIdEntitet(aktørId),
            LocalDate.of(1991, 1, 1).minusYears(30),
            null);
        var sakTittel = ForespørselTekster.lagSaksTittel(personInfo.mapFulltNavn(), personInfo.fødselsdato());

        lenient().when(personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(aktørId), ytelsetype)).thenReturn(personInfo);
        lenient().when(arbeidsgiverNotifikasjon.opprettSak(any(), any(), eq(brregOrgnummer), eq(sakTittel), any())).thenReturn(sakId);
        lenient().when(arbeidsgiverNotifikasjon.opprettOppgave(any(), any(), any(), eq(brregOrgnummer), any(), any(), any(), any()))
            .thenReturn(oppgaveId);
    }
}
