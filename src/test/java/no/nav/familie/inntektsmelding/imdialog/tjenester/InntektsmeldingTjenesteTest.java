package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.bytebuddy.asm.Advice;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;

import no.nav.familie.inntektsmelding.integrasjoner.fpsak.FagsystemTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.fpsak.SøkersFraværsperiode;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;

import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingTjenesteTest {

    private static final String INNMELDER_UID = "12324312345";

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;
    @Mock
    private FpDokgenTjeneste fpDokgenTjeneste;
    @Mock
    private FagsystemTjeneste fagsystemTjeneste;

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    @BeforeAll
    static void beforeAll() {
        KontekstHolder.setKontekst(RequestKontekst.forRequest(INNMELDER_UID, "kompakt", IdentType.EksternBruker,
            new OpenIDToken(OpenIDProvider.TOKENX, new TokenString("token")), UUID.randomUUID(), Set.of()));
    }

    @AfterAll
    static void afterAll() {
        KontekstHolder.fjernKontekst();
    }

    @BeforeEach
    void setUp() {
        inntektsmeldingTjeneste = new InntektsmeldingTjeneste(forespørselBehandlingTjeneste, inntektsmeldingRepository, personTjeneste,
            organisasjonTjeneste, fagsystemTjeneste, inntektTjeneste, fpDokgenTjeneste, prosessTaskTjeneste);
    }

    @Test
    void skal_lage_dto() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"), Ytelsetype.FORELDREPENGER, "123");
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getAktørId(), LocalDate.now(), null));
        var innsenderNavn = "Ine";
        var innsenderEtternavn = "Sender";
        var innsenderTelefonnummer = "+4711111111";
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo(innsenderNavn, null, innsenderEtternavn, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), innsenderTelefonnummer));
        var inntekt1 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 3), BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer());
        var inntekt2 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 4), BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer());
        var inntekt3 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 5), BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer());
        when(inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(), LocalDate.now(),
            forespørsel.getOrganisasjonsnummer())).thenReturn(List.of(inntekt1, inntekt2, inntekt3));
        when(fagsystemTjeneste.hentSøkersFraværsperioder(forespørsel)).thenReturn(List.of(new SøkersFraværsperiode(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15))));

        // Act
        var imDialogDto = inntektsmeldingTjeneste.lagDialogDto(uuid);

        // Assert
        assertThat(imDialogDto.startdatoPermisjon()).isEqualTo(forespørsel.getSkjæringstidspunkt());
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.FORELDREPENGER);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getOrganisasjonsnummer());

        assertThat(imDialogDto.innsender().fornavn()).isEqualTo(innsenderNavn);
        assertThat(imDialogDto.innsender().etternavn()).isEqualTo(innsenderEtternavn);
        assertThat(imDialogDto.innsender().mellomnavn()).isNull();
        assertThat(imDialogDto.innsender().telefon()).isEqualTo(innsenderTelefonnummer);

        assertThat(imDialogDto.søkersFravær().førsteFraværsdag()).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(imDialogDto.søkersFravær().sisteFraværsdag()).isEqualTo(LocalDate.now().plusDays(15));
        assertThat(imDialogDto.søkersFravær().perioder()).hasSize(1);
        assertThat(imDialogDto.søkersFravær().perioder().stream().anyMatch(p -> p.fom().equals(LocalDate.now().plusDays(10)))).isTrue();

        assertThat(imDialogDto.inntekter()).hasSize(3);

        assertThat(imDialogDto.inntekter()).contains(
            new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), BigDecimal.valueOf(52000),
                forespørsel.getOrganisasjonsnummer()));
        assertThat(imDialogDto.inntekter()).contains(
            new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), BigDecimal.valueOf(52000),
                forespørsel.getOrganisasjonsnummer()));
        assertThat(imDialogDto.inntekter()).contains(
            new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31), BigDecimal.valueOf(52000),
                forespørsel.getOrganisasjonsnummer()));
    }

    @Test
    void skal_ikke_godta_im_på_utgått_forespørrsel() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"), Ytelsetype.FORELDREPENGER, "123");
        forespørsel.setStatus(ForespørselStatus.UTGÅTT);
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        var innsendingDto = new SendInntektsmeldingRequestDto(uuid,
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Navn", "123"),
            LocalDate.now(),
            BigDecimal.valueOf(10000),
            List.of(),
            List.of(),
            List.of());

        // Act
        var ex = assertThrows(IllegalStateException.class, () -> inntektsmeldingTjeneste.mottaInntektsmelding(innsendingDto));

        // Assert
        assertThat(ex.getMessage()).contains("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");

    }
}
