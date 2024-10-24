package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;

import no.nav.familie.inntektsmelding.koder.ForespørselStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class InntektsmeldingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;
    private FpDokgenTjeneste fpDokgenTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    InntektsmeldingTjeneste() {
    }

    @Inject
    public InntektsmeldingTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                   InntektsmeldingRepository inntektsmeldingRepository,
                                   PersonTjeneste personTjeneste,
                                   OrganisasjonTjeneste organisasjonTjeneste,
                                   InntektTjeneste inntektTjeneste,
                                   FpDokgenTjeneste fpDokgenTjeneste,
                                   ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.fpDokgenTjeneste = fpDokgenTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public InntektsmeldingResponseDto mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var aktorId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto);
        var imId = lagreOgLagJournalførTask(entitet);
        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(mottattInntektsmeldingDto.foresporselUuid());
        var erTilhørendeForespørselUtgått = forespørselEntitet.map(f -> ForespørselStatus.UTGÅTT.equals(f.getStatus())).orElse(false);
        if (erTilhørendeForespørselUtgått) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }
        var lukketForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktorId, orgnummer,
            mottattInntektsmeldingDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING);

        var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

        // Metrikker i prometheus
        MetrikkerTjeneste.loggForespørselLukkIntern(lukketForespørsel);
        MetrikkerTjeneste.loggInnsendtInntektsmelding(imEntitet);

        return InntektsmeldingMapper.mapFraEntitet(imEntitet, mottattInntektsmeldingDto.foresporselUuid());
    }

    private Long lagreOgLagJournalførTask(InntektsmeldingEntitet entitet) {
        var imId = inntektsmeldingRepository.lagreInntektsmelding(entitet);
        opprettTaskForSendTilJoark(imId);
        return imId;
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
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));
        var personDto = lagPersonDto(forespørsel);
        var organisasjonDto = lagOrganisasjonDto(forespørsel);
        var innmelderDto = lagInnmelderDto(forespørsel.getYtelseType());
        var inntektDtoer = lagInntekterDto(forespørsel);
        return new InntektsmeldingDialogDto(personDto, organisasjonDto, innmelderDto, inntektDtoer, forespørsel.getSkjæringstidspunkt(),
            KodeverkMapper.mapYtelsetype(forespørsel.getYtelseType()), forespørsel.getUuid(), KodeverkMapper.mapForespørselStatus(forespørsel.getStatus()));
    }

    public InntektsmeldingEntitet hentInntektsmelding(long inntektsmeldingId) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
    }

    public List<InntektsmeldingResponseDto> hentInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));

        var inntektsmeldinger = inntektsmeldingRepository.hentInntektsmeldinger(forespørsel.getAktørId(),
            forespørsel.getOrganisasjonsnummer(),
            forespørsel.getSkjæringstidspunkt(),
            forespørsel.getYtelseType());
        return inntektsmeldinger.stream().map(im -> InntektsmeldingMapper.mapFraEntitet(im, forespørsel.getUuid())).toList();
    }

    public byte[] hentPDF(long id) {
        var inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(id);
        return fpDokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet);
    }

    private InntektsmeldingDialogDto.InnsenderDto lagInnmelderDto(Ytelsetype ytelsetype) {
        if (!KontekstHolder.harKontekst() || !IdentType.EksternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new IllegalStateException("Mangler innlogget bruker kontekst.");
        }
        var pid = KontekstHolder.getKontekst().getUid();
        var personInfo = personTjeneste.hentPersonFraIdent(PersonIdent.fra(pid), ytelsetype);
        return new InntektsmeldingDialogDto.InnsenderDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.telefonnummer());
    }

    private List<InntektsmeldingDialogDto.MånedsinntektResponsDto> lagInntekterDto(ForespørselEntitet forespørsel) {
        var inntekter = inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(), LocalDate.now(),
            forespørsel.getOrganisasjonsnummer());
        return inntekter.stream()
            .map(i -> new InntektsmeldingDialogDto.MånedsinntektResponsDto(i.måned().atDay(1), i.måned().atEndOfMonth(), i.beløp(),
                forespørsel.getOrganisasjonsnummer()))
            .toList();
    }

    private InntektsmeldingDialogDto.OrganisasjonInfoResponseDto lagOrganisasjonDto(ForespørselEntitet forespørsel) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer());
        return new InntektsmeldingDialogDto.OrganisasjonInfoResponseDto(orgdata.navn(), orgdata.orgnr());
    }

    private InntektsmeldingDialogDto.PersonInfoResponseDto lagPersonDto(ForespørselEntitet forespørsel) {
        var persondata = personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType());
        return new InntektsmeldingDialogDto.PersonInfoResponseDto(persondata.fornavn(), persondata.mellomnavn(), persondata.etternavn(),
            persondata.fødselsnummer().getIdent(), persondata.aktørId().getAktørId());
    }
}
