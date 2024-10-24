package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.database.JpaTestcontainerExtension;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(JpaTestcontainerExtension.class)
public class ForespørselBehandlingTjenesteImplTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String AKTØR_ID = "1234567891234";
    private static final String SAK_ID = "1";
    private static final String OPPGAVE_ID = "2";
    private static final String SAK_ID_2 = "3";
    private static final String OPPGAVE_ID_2 = "4";
    private static final String SAKSNUMMMER = "FAGSAK_SAKEN";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusYears(1);
    private static final Ytelsetype YTELSETYPE = Ytelsetype.PLEIEPENGER_SYKT_BARN;

    private final ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon = Mockito.mock(ArbeidsgiverNotifikasjon.class);
    private final PersonTjeneste personTjeneste = Mockito.mock(PersonTjeneste.class);
    private ForespørselRepository forespørselRepository;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @BeforeEach
    public void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        this.forespørselBehandlingTjeneste = new ForespørselBehandlingTjenesteImpl(new ForespørselTjeneste(forespørselRepository),
            arbeidsgiverNotifikasjon,
            personTjeneste);
    }

    @Test
    public void skal_opprette_forespørsel_og_sette_sak_og_oppgave() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER));

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));

        assertThat(resultat).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
        assertThat(lagret.size()).isEqualTo(1);
        assertThat(lagret.getFirst().getArbeidsgiverNotifikasjonSakId()).isEqualTo(SAK_ID);
        assertThat(lagret.getFirst().getOppgaveId()).isEqualTo(OPPGAVE_ID);
    }


    @Test
    public void eksisterende_åpen_forespørsel_skal_gi_noop() {
        forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);

        getEntityManager().clear();

        var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER));

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));
        assertThat(resultat).isEqualTo(ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE_ÅPEN);
        assertThat(lagret.size()).isEqualTo(1);
    }

    @Test
    public void skal_ferdigstille_forespørsel() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
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
    public void skal_sette_alle_forespørspørsler_for_sak_til_ferdig() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.FERDIG);
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.get().getStatus()).isEqualTo(ForespørselStatus.FERDIG);
    }

    @Test
    public void skal_sette_alle_forespørspørsler_for_sak_til_utgått() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.settForespørselTilUtgått(new SaksnummerDto(SAKSNUMMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.get().getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);
    }

    @Test
    public void skal_lukke_forespørsel_for_sak_med_gitt_stp() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER);
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
    public void skal_opprette_forespørsel_dersom_det_ikke_eksisterer_en_for_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var orgPerStp = new HashMap<LocalDate, List<OrganisasjonsnummerDto>>() {{
            put(SKJÆRINGSTIDSPUNKT, List.of(new OrganisasjonsnummerDto(BRREG_ORGNUMMER)));
        }};
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), orgPerStp, new SaksnummerDto(SAKSNUMMMER));

        clearHibernateCache();

        var forespørslerForFagsak = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));
        assertThat(forespørslerForFagsak.size()).isEqualTo(1);
        assertThat(forespørslerForFagsak.getFirst().getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
    }

    @Test
    public void skal_ikke_opprette_ny_forespørsel_dersom_det_eksisterer_en_for_samme_stp() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var orgPerStp = new HashMap<LocalDate, List<OrganisasjonsnummerDto>>() {{
            put(SKJÆRINGSTIDSPUNKT, List.of(new OrganisasjonsnummerDto(BRREG_ORGNUMMER)));
        }};
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), orgPerStp, new SaksnummerDto(SAKSNUMMMER));

        clearHibernateCache();

        var forespørslerForFagsak = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));
        assertThat(forespørslerForFagsak.size()).isEqualTo(1);
        assertThat(forespørslerForFagsak.getFirst().getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
    }

    @Test
    public void skal_opprette_ny_forespørsel_og_beholde_gammel_dersom_vi_ber_om_et_nytt_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var orgPerStp = new HashMap<LocalDate, List<OrganisasjonsnummerDto>>() {{
            put(SKJÆRINGSTIDSPUNKT, List.of(new OrganisasjonsnummerDto(BRREG_ORGNUMMER)));
            put(SKJÆRINGSTIDSPUNKT.plusDays(10), List.of(new OrganisasjonsnummerDto(BRREG_ORGNUMMER)));
        }};
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), orgPerStp, new SaksnummerDto(SAKSNUMMMER));

        clearHibernateCache();

        var forespørslerForFagsak = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));
        assertThat(forespørslerForFagsak.size()).isEqualTo(2);
        assertThat(forespørslerForFagsak.get(0).getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(forespørslerForFagsak.get(1).getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
    }

    @Test
    public void skal_opprette_ny_forespørsel_og_markere_gammel_som_utgått_dersom_vi_erstatter_stp() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var orgPerStp = new HashMap<LocalDate, List<OrganisasjonsnummerDto>>() {{
            put(SKJÆRINGSTIDSPUNKT.plusDays(10), List.of(new OrganisasjonsnummerDto(BRREG_ORGNUMMER)));
        }};

        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID_2, OPPGAVE_ID_2);
        forespørselBehandlingTjeneste.oppdaterForespørsler(YTELSETYPE, new AktørIdEntitet(AKTØR_ID), orgPerStp, new SaksnummerDto(SAKSNUMMMER));

        clearHibernateCache();

        var forespørslerForFagsak = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));
        var utgåtteForespørsler = forespørslerForFagsak.stream().filter(f -> f.getStatus() == ForespørselStatus.UTGÅTT).toList();
        var forespørslerUnderBehandling = forespørslerForFagsak.stream().filter(f -> f.getStatus() == ForespørselStatus.UNDER_BEHANDLING).toList();
        assertThat(forespørslerForFagsak.size()).isEqualTo(2);
        assertThat(forespørslerUnderBehandling.size()).isEqualTo(1);
        assertThat(utgåtteForespørsler.size()).isEqualTo(1);
    }

    @Test
    public void skal_slette_oppgave_gitt_saksnummer_og_orgnr() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        when(arbeidsgiverNotifikasjon.slettSak(SAK_ID)).thenReturn(SAK_ID);

        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);
        verify(arbeidsgiverNotifikasjon, Mockito.times(1)).slettSak(SAK_ID);
    }

    @Test
    public void skal_slette_oppgave_gitt_saksnummer() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        when(arbeidsgiverNotifikasjon.slettSak(SAK_ID)).thenReturn(SAK_ID);

        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(SAKSNUMMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);
        verify(arbeidsgiverNotifikasjon, Mockito.times(1)).slettSak(SAK_ID);
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

        when(personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(aktørId), ytelsetype)).thenReturn(personInfo);
        when(arbeidsgiverNotifikasjon.opprettSak(any(), any(), eq(brregOrgnummer), eq(sakTittel), any())).thenReturn(sakId);
        when(arbeidsgiverNotifikasjon.opprettOppgave(any(), any(), any(), eq(brregOrgnummer), any(), any(), any(), any())).thenReturn(oppgaveId);
    }
}
