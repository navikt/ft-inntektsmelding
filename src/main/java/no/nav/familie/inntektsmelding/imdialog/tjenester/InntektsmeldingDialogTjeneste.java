package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.SakEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class InntektsmeldingDialogTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingDialogTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    InntektsmeldingDialogTjeneste() {
    }

    @Inject
    public InntektsmeldingDialogTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         InntektsmeldingRepository inntektsmeldingRepository,
                                         PersonTjeneste personTjeneste,
                                         OrganisasjonTjeneste organisasjonTjeneste,
                                         InntektTjeneste inntektTjeneste,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var aktorId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto);
        var imId = inntektsmeldingRepository.lagreInntektsmelding(entitet);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktorId, orgnummer, mottattInntektsmeldingDto.startdato());
        opprettTaskForSendTilJoark(imId);
    }

    private void opprettTaskForSendTilJoark(Long imId) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setCallIdFraEksisterende();
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }

    public InntektsmeldingDialogDto lagDialogDto(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));
        var personDto = lagPersonDto(forespørsel.getSak());
        var organisasjonDto = lagOrganisasjonDto(forespørsel.getSak());
        var inntektDtoer = lagInntekterDto(forespørsel);
        var sak = forespørsel.getSak();
        return new InntektsmeldingDialogDto(personDto, organisasjonDto, inntektDtoer,
            forespørsel.getSkjæringstidspunkt(), KodeverkMapper.mapYtelsetype(sak.getYtelseType()), forespørsel.getUuid());
    }

    public InntektsmeldingEntitet hentInntektsmelding(int inntektsmeldingId) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
    }

    private List<InntektsmeldingDialogDto.MånedsinntektResponsDto> lagInntekterDto(ForespørselEntitet forespørsel) {
        var sak = forespørsel.getSak();
        var inntekter = inntektTjeneste.hentInntekt(sak.getAktørId(), forespørsel.getSkjæringstidspunkt(),
            sak.getOrganisasjonsnummer());
        var inntektDtoer = inntekter.stream()
            .map(i -> new InntektsmeldingDialogDto.MånedsinntektResponsDto(i.måned().atDay(1), i.måned().atEndOfMonth(), i.beløp(), sak.getOrganisasjonsnummer()))
            .toList();
        return inntektDtoer;
    }

    private InntektsmeldingDialogDto.OrganisasjonInfoResponseDto lagOrganisasjonDto(SakEntitet sak) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(sak.getOrganisasjonsnummer());
        var organisasjonDto = new InntektsmeldingDialogDto.OrganisasjonInfoResponseDto(orgdata.navn(), orgdata.orgnr());
        return organisasjonDto;
    }

    private InntektsmeldingDialogDto.PersonInfoResponseDto lagPersonDto(SakEntitet sak) {
        var persondata = personTjeneste.hentPersonInfo(sak.getAktørId(), sak.getYtelseType());
        var personDto = new InntektsmeldingDialogDto.PersonInfoResponseDto(
            persondata.fornavn(),
            persondata.mellomnavn(),
            persondata.etternavn(),
            persondata.fødselsnummer().getIdent(),
            persondata.aktørId().getAktørId());
        return personDto;
    }

}
