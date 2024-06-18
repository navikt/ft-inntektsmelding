package no.nav.familie.inntektsmelding.integrasjoner.joark;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Behandlingtema;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.AvsenderMottaker;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Bruker;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.DokumentInfoOpprett;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Dokumentvariant;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.OpprettJournalpostRequest;

@ApplicationScoped
public class JoarkTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(JoarkTjeneste.class);
    // Ved maskinell journalføring skal enhet være satt til 9999. Se https://confluence.adeo.no/display/BOA/opprettJournalpost
    private static final String JOURNALFØRENDE_ENHET = "9999";
    private static final String JOURNALFØRING_TITTEL = "Inntektsmelding";
    // TODO Dette er brevkode for altinn skjema. Trenger vi egen?
    private static final String BREVKODE_IM = "4936";

    private JoarkKlient joarkKlient;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private PersonTjeneste personTjeneste;

    JoarkTjeneste() {
        // CDI
    }

    @Inject
    public JoarkTjeneste(JoarkKlient joarkKlient, OrganisasjonTjeneste organisasjonTjeneste, PersonTjeneste personTjeneste) {
        this.joarkKlient = joarkKlient;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.personTjeneste = personTjeneste;
    }


    public void journalførInntektsmelding(String XMLAvInntektsmelding, InntektsmeldingEntitet inntektsmelding) {
        var request = opprettRequest(XMLAvInntektsmelding, inntektsmelding);
        try {
            var response = joarkKlient.opprettJournalpost(request, true);
            // Kan nok fjerne loggingen etter en periode i dev, mest for feilsøking i starten.
            LOG.info("Journalført inntektsmelding fikk journalpostId " + response.journalpostId());
            LOG.info("Ble journalført inntektsmelding ferdigstilt:  " + response.journalpostferdigstilt());
        } catch (Exception e) {
            throw new IllegalStateException("Klarte ikke journalføre innteketsmelding " + e);
        }
    }

    private OpprettJournalpostRequest opprettRequest(String xmlAvInntektsmelding, InntektsmeldingEntitet inntektsmeldingEntitet) {
        boolean erBedrift = inntektsmeldingEntitet.getArbeidsgiverIdent().length() == 9;
        AvsenderMottaker avsenderMottaker = erBedrift ? lagAvsenderBedrift(inntektsmeldingEntitet) : lagAvsenderPrivatperson(inntektsmeldingEntitet);
        var request = OpprettJournalpostRequest.nyInngående()
            .medTittel(JOURNALFØRING_TITTEL)
            .medAvsenderMottaker(avsenderMottaker)
            .medBruker(lagBruker(inntektsmeldingEntitet.getAktørId()))
            .medBehandlingstema(mapBehandlingTema(inntektsmeldingEntitet.getYtelsetype()))
            .medDatoMottatt(inntektsmeldingEntitet.getOpprettetTidspunkt().toLocalDate())
            .medTema(mapTema(inntektsmeldingEntitet.getYtelsetype()))
            .medEksternReferanseId(UUID.randomUUID().toString())
            .medJournalfoerendeEnhet(JOURNALFØRENDE_ENHET)
            .medKanal("NAV_NO") // TODO Denne bør nok synkes med avsendersystem i XML
            .medDokumenter(lagDokumenter(xmlAvInntektsmelding))
            .build();
        return request;
    }

    private List<DokumentInfoOpprett> lagDokumenter(String xmlAvInntektsmelding) {
        var dokumentvariant = new Dokumentvariant(Dokumentvariant.Variantformat.ORIGINAL, Dokumentvariant.Filtype.XML,
            xmlAvInntektsmelding.getBytes(StandardCharsets.UTF_8)); // TODO Legg til PDF i denne lista

        var builder = DokumentInfoOpprett.builder()
            .medTittel(JOURNALFØRING_TITTEL)
            .medBrevkode(BREVKODE_IM)
            .leggTilDokumentvariant(dokumentvariant);

        return Collections.singletonList(builder.build());
    }

    private String mapTema(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER, SVANGERSKAPSPENGER -> "FORS_SVA";
            case PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OMSORGSPENGER, OPPLÆRINGSPENGER -> "OMS";
        };
    }

    private String mapBehandlingTema(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> Behandlingtema.FORELDREPENGER.getOffisiellKode();
            case SVANGERSKAPSPENGER -> Behandlingtema.SVANGERSKAPSPENGER.getOffisiellKode();
            case PLEIEPENGER_SYKT_BARN -> Behandlingtema.PLEIEPENGER_SYKT_BARN.getOffisiellKode();
            case PLEIEPENGER_NÆRSTÅENDE -> Behandlingtema.PLEIEPENGER_LIVETS_SLUTTFASE.getOffisiellKode();
            case OMSORGSPENGER -> Behandlingtema.OMSORGSPENGER.getOffisiellKode();
            case OPPLÆRINGSPENGER -> throw new IllegalArgumentException("Finner ikke behandlingtema for ytelsetype " + ytelsetype); // TODO Hva skal inn her?
        };
    }

    private Bruker lagBruker(AktørIdEntitet aktørId) {
        return new Bruker(aktørId.getAktørId(), Bruker.BrukerIdType.AKTOERID);
    }

    private AvsenderMottaker lagAvsenderPrivatperson(InntektsmeldingEntitet inntektsmeldingEntitet) {
        var personInfo = personTjeneste.hentPersonInfo(new AktørIdEntitet(inntektsmeldingEntitet.getArbeidsgiverIdent()),
            inntektsmeldingEntitet.getYtelsetype());
        return new AvsenderMottaker(personInfo.fødselsnummer().getIdent(), AvsenderMottaker.AvsenderMottakerIdType.FNR, personInfo.navn());
    }

    private AvsenderMottaker lagAvsenderBedrift(InntektsmeldingEntitet inntektsmeldingEntitet) {
        var org = organisasjonTjeneste.finnOrganisasjon(inntektsmeldingEntitet.getArbeidsgiverIdent());
        return new AvsenderMottaker(org.orgnr(), AvsenderMottaker.AvsenderMottakerIdType.ORGNR, org.navn());
    }


}
