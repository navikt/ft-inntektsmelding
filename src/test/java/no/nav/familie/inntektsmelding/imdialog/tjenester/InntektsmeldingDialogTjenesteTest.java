package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.modell.SakEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingDialogTjenesteTest {
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
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste;

    @BeforeEach
    void setUp() {
        inntektsmeldingDialogTjeneste = new InntektsmeldingDialogTjeneste(forespørselBehandlingTjeneste, inntektsmeldingRepository,
            personTjeneste, organisasjonTjeneste, inntektTjeneste, prosessTaskTjeneste);
    }

    @Test
    void skal_lage_dto() {
        // Arrange
        var uuid = UUID.randomUUID();
        var sakEntitet = new SakEntitet("999999999", new AktørIdEntitet("9999999999999"), Ytelsetype.FORELDREPENGER, "SAKEN");
        var forespørsel = new ForespørselEntitet(sakEntitet, LocalDate.now());

        when(forespørselBehandlingTjeneste.hentForespørsel(uuid))
            .thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getSak().getOrganisasjonsnummer()))
            .thenReturn(new Organisasjon("Bedriften", forespørsel.getSak().getOrganisasjonsnummer()));

        when(personTjeneste.hentPersonInfo(forespørsel.getSak().getAktørId(), forespørsel.getSak().getYtelseType()))
            .thenReturn(new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getSak().getAktørId(), LocalDate.now()));
        var inntekt1 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 3), BigDecimal.valueOf(52000), forespørsel.getSak().getOrganisasjonsnummer());
        var inntekt2 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 4), BigDecimal.valueOf(52000), forespørsel.getSak().getOrganisasjonsnummer());
        var inntekt3 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 5), BigDecimal.valueOf(52000), forespørsel.getSak().getOrganisasjonsnummer());
        when(inntektTjeneste.hentInntekt(forespørsel.getSak().getAktørId(), forespørsel.getSkjæringstidspunkt(), forespørsel.getSak().getOrganisasjonsnummer()))
            .thenReturn(List.of(inntekt1, inntekt2, inntekt3));

        // Act
        var imDialogDto = inntektsmeldingDialogTjeneste.lagDialogDto(uuid);

        // Assert
        assertThat(imDialogDto.startdatoPermisjon()).isEqualTo(forespørsel.getSkjæringstidspunkt());
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.FORELDREPENGER);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getSak().getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getSak().getOrganisasjonsnummer());

        assertThat(imDialogDto.inntekter()).hasSize(3);

        assertThat(imDialogDto.inntekter())
            .contains(new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024,3,1), LocalDate.of(2024, 3, 31), BigDecimal.valueOf(52000), forespørsel.getSak().getOrganisasjonsnummer()));
        assertThat(imDialogDto.inntekter())
            .contains(new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024,4,1), LocalDate.of(2024, 4, 30), BigDecimal.valueOf(52000), forespørsel.getSak().getOrganisasjonsnummer()));
        assertThat(imDialogDto.inntekter())
            .contains(new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024,5,1), LocalDate.of(2024, 5, 31), BigDecimal.valueOf(52000), forespørsel.getSak().getOrganisasjonsnummer()));
    }


}
