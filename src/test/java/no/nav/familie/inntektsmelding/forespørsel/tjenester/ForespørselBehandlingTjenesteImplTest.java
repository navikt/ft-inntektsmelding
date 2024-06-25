package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselOgSakRepository;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.SakStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class ForespørselBehandlingTjenesteImplTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String AKTØR_ID = "1234567891234";
    private static final String SAK_ID = "1";
    private static final String OPPGAVE_ID = "2";
    private static final String SAKSNUMMMER = "FAGSAK_SAKEN";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusYears(1);
    private static final Ytelsetype YTELSETYPE = Ytelsetype.PLEIEPENGER_SYKT_BARN;


    @Inject
    private EntityManager entityManager;
    private final ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon = Mockito.mock(ArbeidsgiverNotifikasjon.class);
    private final PersonTjeneste personTjeneste = Mockito.mock(PersonTjeneste.class);
    private ForespørselOgSakRepository forespørselRepository;
    private ForespørselBehandlingTjenesteImpl forespørselBehandlingTjeneste;


    @BeforeEach
    public void setUp() {
        this.forespørselRepository = new ForespørselOgSakRepository(entityManager);
        this.forespørselBehandlingTjeneste = new ForespørselBehandlingTjenesteImpl(new ForespørselTjeneste(forespørselRepository),
            arbeidsgiverNotifikasjon, personTjeneste);

    }

    @Test
    public void skal_opprette_forespørsel_og_sette_sak_og_oppgave() {
        var personInfo = new PersonInfo("Navn", null, "Navnesen", new PersonIdent("01019100000"), new AktørIdEntitet(AKTØR_ID),
            LocalDate.of(1991, 1, 1).minusYears(30));

        var saksTittel = forespørselBehandlingTjeneste.lagSaksTittel(personInfo);


        when(personTjeneste.hentPersonInfo(new AktørIdEntitet(AKTØR_ID), YTELSETYPE)).thenReturn(personInfo);
        when(arbeidsgiverNotifikasjon.opprettSak(any(), any(), eq(BRREG_ORGNUMMER), eq(saksTittel), any())).thenReturn(SAK_ID);
        when(arbeidsgiverNotifikasjon.opprettOppgave(any(), any(), any(), eq(BRREG_ORGNUMMER), any(), any())).thenReturn(OPPGAVE_ID);

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER), new SaksnummerDto(SAKSNUMMMER));

        var lagret = forespørselRepository.finnÅpenForespørsel(new AktørIdEntitet(AKTØR_ID), YTELSETYPE, BRREG_ORGNUMMER, SAKSNUMMMER, SKJÆRINGSTIDSPUNKT);

        assertThat(lagret.get().getSak().getFagerSakId()).isEqualTo(SAK_ID);
        assertThat(lagret.get().getOppgaveId()).isEqualTo(OPPGAVE_ID);
    }


    @Test
    public void eksisterende_åpen_forespørsel_skal_gi_noop() {
        forespørselRepository.opprettSak(Ytelsetype.PLEIEPENGER_SYKT_BARN, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER), new SaksnummerDto(SAKSNUMMMER));


        var lagret = forespørselRepository.finnÅpenForespørsel(new AktørIdEntitet(AKTØR_ID), YTELSETYPE, BRREG_ORGNUMMER, SAKSNUMMMER, SKJÆRINGSTIDSPUNKT);
        assertThat(lagret.isPresent()).isTrue();
    }

    @Test
    public void skal_ferdigstille_forespørsel_og_sak() {
        var sakEntitet = forespørselRepository.opprettSak(Ytelsetype.PLEIEPENGER_SYKT_BARN, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterOppgaveId(forespørselUuid, OPPGAVE_ID);
        forespørselRepository.oppdaterSakId(sakEntitet.getId(), SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid, new AktørIdEntitet(AKTØR_ID), new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT);

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.get().getForespørselStatus()).isEqualTo(ForespørselStatus.UTFOERT);
    }

    @Test
    public void skal_ferdigstille_sak() {
        var sakEntitet = forespørselRepository.opprettSak(Ytelsetype.PLEIEPENGER_SYKT_BARN, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER);
        forespørselRepository.oppdaterOppgaveId(forespørselUuid, OPPGAVE_ID);
        forespørselRepository.oppdaterSakId(sakEntitet.getId(), SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillSak(new AktørIdEntitet(AKTØR_ID), new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            new SaksnummerDto(SAKSNUMMMER));

        var lagret = forespørselRepository.hentSak(sakEntitet.getId());
        assertThat(lagret.getSakStatus()).isEqualTo(SakStatus.FERDIG);
    }

}
