package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
class ArbeidsgiverNotifikasjonTjeneste implements ArbeidsgiverNotifikasjon {

    static final String SERVICE_CODE = "4936";
    static final String SERVICE_EDITION_CODE = "1";

    private ArbeidsgiverNotifikasjonKlient klient;

    public ArbeidsgiverNotifikasjonTjeneste() {
    }

    @Inject
    public ArbeidsgiverNotifikasjonTjeneste(ArbeidsgiverNotifikasjonKlient klient) {
        this.klient = klient;
    }

    @Override
    public HentetSak hentSakMedGrupperingsid(String grupperingsid, Merkelapp merkelapp) {
        var request = new HentSakMedGrupperingsidQueryRequest();
        request.setGrupperingsid(grupperingsid);
        request.setMerkelapp(merkelapp.getBeskrivelse());

        var projection = new HentSakResultatResponseProjection().typename()
            .onHentetSak(new HentetSakResponseProjection().sak(new SakResponseProjection().id()
                .grupperingsid()
                .merkelapp()
                .lenke()
                .tittel()
                .virksomhetsnummer()))
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.hentSakMedGrupperingsid(request, projection);
    }

    @Override
    public HentetSak hentSak(String sakId) {
        var request = new HentSakQueryRequest();
        request.setId(sakId);

        var projection = new HentSakResultatResponseProjection().typename()
            .onHentetSak(new HentetSakResponseProjection().sak(new SakResponseProjection().id()
                .grupperingsid()
                .merkelapp()
                .lenke()
                .tittel()
                .virksomhetsnummer()))
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.hentSak(request, projection);
    }

    @Override
    public String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke, String statusTekst) {

        var request = new NySakMutationRequest();

        request.setGrupperingsid(grupperingsid);
        request.setTittel(saksTittel);
        request.setVirksomhetsnummer(virksomhetsnummer);
        request.setMerkelapp(merkelapp.getBeskrivelse());
        request.setLenke(lenke.toString());
        request.setInitiellStatus(SaksStatus.UNDER_BEHANDLING);
        request.setOverstyrStatustekstMed(statusTekst);
        request.setMottakere(List.of(new MottakerInput(new AltinnMottakerInput(SERVICE_CODE, SERVICE_EDITION_CODE), null)));

        var projection = new NySakResultatResponseProjection().typename()
            .onNySakVellykket(new NySakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatGrupperingsid(new DuplikatGrupperingsidResponseProjection().feilmelding())
            .onDuplikatGrupperingsidEtterDelete(new DuplikatGrupperingsidEtterDeleteResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding());

        return klient.opprettSak(request, projection);
    }

    @Override
    public String oppdaterSakStatus(String sakId, SaksStatus status, String overstyrtStatusText) {

        var request = new NyStatusSakMutationRequest();
        request.setId(sakId);
        request.setNyStatus(status);
        request.setOverstyrStatustekstMed(overstyrtStatusText);

        var projection = new NyStatusSakResultatResponseProjection().typename()
            .onNyStatusSakVellykket(new NyStatusSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onKonflikt(new KonfliktResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());

        return klient.oppdaterSakStatus(request, projection);
    }

    @Override
    public String oppdaterSakStatusMedGrupperingsId(String grupperingsid, Merkelapp merkelapp, SaksStatus status, String overstyrtStatusText) {
        var request = new NyStatusSakByGrupperingsidMutationRequest();
        request.setGrupperingsid(grupperingsid);
        request.setMerkelapp(merkelapp.getBeskrivelse());
        request.setNyStatus(status);
        request.setOverstyrStatustekstMed(overstyrtStatusText);

        var projection = new NyStatusSakResultatResponseProjection().typename()
            .onNyStatusSakVellykket(new NyStatusSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onKonflikt(new KonfliktResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());

        return klient.oppdaterSakStatusMedGrupperingsid(request, projection);
    }

    @Override
    public String opprettOppgave(String grupperingsid,
                                 Merkelapp oppgaveMerkelapp,
                                 String eksternId,
                                 String virksomhetsnummer,
                                 String oppgaveTekst,
                                 String varselTekst,
                                 URI oppgaveLenke) {

        var request = NyOppgaveMutationRequest.builder()
            .setNyOppgave(NyOppgaveInput.builder()
                .setMottaker(MottakerInput.builder()
                    .setAltinn(AltinnMottakerInput.builder().setServiceCode(SERVICE_CODE).setServiceEdition(SERVICE_EDITION_CODE).build())
                    .build())
                .setNotifikasjon(NotifikasjonInput.builder()
                    .setMerkelapp(oppgaveMerkelapp.getBeskrivelse())
                    .setTekst(oppgaveTekst)
                    .setLenke(oppgaveLenke.toString())
                    .build())
                .setMetadata(MetadataInput.builder()
                    .setVirksomhetsnummer(virksomhetsnummer)
                    .setEksternId(eksternId)
                    .setGrupperingsid(grupperingsid)
                    .build())
                .setEksterneVarsler(lagEksternVarselAltinn(varselTekst))
                .build())
            .build();


        var projection = new NyOppgaveResultatResponseProjection().typename()
            .onNyOppgaveVellykket(new NyOppgaveVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatEksternIdOgMerkelapp(new DuplikatEksternIdOgMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding())
            .onUgyldigPaaminnelseTidspunkt(new UgyldigPaaminnelseTidspunktResponseProjection().feilmelding());

        return klient.opprettOppgave(request, projection);
    }

    private List<EksterntVarselInput> lagEksternVarselAltinn(String varselTekst) {
        var altinnVarsel = EksterntVarselAltinntjenesteInput.builder()
            .setTittel("Du har fått en oppgave fra NAV")
            .setInnhold(varselTekst)
            .setMottaker(AltinntjenesteMottakerInput.builder().setServiceCode(SERVICE_CODE).setServiceEdition(SERVICE_EDITION_CODE).build())
            .setSendetidspunkt(SendetidspunktInput.builder().setSendevindu(Sendevindu.LOEPENDE).build())
            .build();
        var eksternVarsel = EksterntVarselInput.builder().setAltinntjeneste(altinnVarsel).build();
        return Collections.singletonList(eksternVarsel);
    }

    @Override
    public String oppgaveUtført(String oppgaveId, OffsetDateTime tidspunkt) {

        var request = OppgaveUtfoertMutationRequest.builder()
            .setId(oppgaveId)
            .setUtfoertTidspunkt(tidspunkt.format(DateTimeFormatter.ISO_DATE_TIME))
            .build();

        var projection = new OppgaveUtfoertResultatResponseProjection().typename()
            .onOppgaveUtfoertVellykket(new OppgaveUtfoertVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.oppgaveUtført(request, projection);
    }

    @Override
    public String oppgaveUtførtByEksternId(String eksternId, Merkelapp merkelapp, OffsetDateTime tidspunkt) {

        var request = OppgaveUtfoertByEksternId_V2MutationRequest.builder()
            .setEksternId(eksternId)
            .setMerkelapp(merkelapp.getBeskrivelse())
            .setUtfoertTidspunkt(tidspunkt.format(DateTimeFormatter.ISO_DATE_TIME))
            .build();

        var projection = new OppgaveUtfoertResultatResponseProjection().typename()
            .onOppgaveUtfoertVellykket(new OppgaveUtfoertVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.oppgaveUtførtByEksternId(request, projection);
    }

    @Override
    public String oppgaveUtgått(String oppgaveId, OffsetDateTime tidspunkt) {

        var request = OppgaveUtgaattMutationRequest.builder()
            .setId(oppgaveId)
            .setUtgaattTidspunkt(tidspunkt.format(DateTimeFormatter.ISO_DATE_TIME))
            .build();

        var projection = new OppgaveUtgaattResultatResponseProjection().typename()
            .onOppgaveUtgaattVellykket(new OppgaveUtgaattVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.oppgaveUtgått(request, projection);
    }

    @Override
    public String ferdigstillSak(String id, String overstyrStatustekst) {

        var request = NyStatusSakMutationRequest.builder()
            .setId(id)
            .setNyStatus(SaksStatus.FERDIG)
            .setOverstyrStatustekstMed(overstyrStatustekst)
            .build();


        var projection = new NyStatusSakResultatResponseProjection().typename()
            .onNyStatusSakVellykket(new NyStatusSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onKonflikt(new KonfliktResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());

        return klient.oppdaterSakStatus(request, projection);
    }

    @Override
    public String oppdaterSakTilleggsinformasjon(String id, String overstyrtTilleggsinformasjon) {
        var request = TilleggsinformasjonSakMutationRequest.builder()
            .setId(id)
            .setTilleggsinformasjon(overstyrtTilleggsinformasjon)
            .build();

        var projection = new TilleggsinformasjonSakResultatResponseProjection().typename()
            .onTilleggsinformasjonSakVellykket(new TilleggsinformasjonSakVellykketResponseProjection().id())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding())
            .onKonflikt(new KonfliktResponseProjection().feilmelding())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.oppdaterSakTilleggsinformasjon(request, projection);
    }

}
