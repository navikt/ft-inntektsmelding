package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
            .onHentetSak(new HentetSakResponseProjection().sak(
                new SakResponseProjection().id().grupperingsid().merkelapp().lenke().tittel().virksomhetsnummer()))
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
            .onHentetSak(new HentetSakResponseProjection().sak(
                new SakResponseProjection().id().grupperingsid().merkelapp().lenke().tittel().virksomhetsnummer()))
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.hentSak(request, projection);
    }

    @Override
    public String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke) {

        var request = new NySakMutationRequest();

        request.setGrupperingsid(grupperingsid);
        request.setTittel(saksTittel);
        request.setVirksomhetsnummer(virksomhetsnummer);
        request.setMerkelapp(merkelapp.getBeskrivelse());
        request.setLenke(lenke != null ? lenke.toString() : null);
        request.setInitiellStatus(SaksStatus.UNDER_BEHANDLING);
        request.setOverstyrStatustekstMed("NAV trenger inntektsmelding");
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
        //request.setNyLenkeTilSak("lenke");

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
        //request.setNyLenkeTilSak("lenke");

        var projection = new NyStatusSakResultatResponseProjection().typename()
            .onNyStatusSakVellykket(new NyStatusSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onKonflikt(new KonfliktResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());

        return klient.oppdaterSakStatusMedGrupperingsid(request, projection);
    }

    @Override
    public String opprettOppgave(String grupperingsid, Merkelapp notifikasjonMerkelapp, String eksternId, String virksomhetsnummer,
                                 String notifikasjonTekst,
                                 URI notifikasjonLenke) {

        var request = new NyOppgaveMutationRequest();
        var input = new NyOppgaveInput();
        input.setMottaker(new MottakerInput(new AltinnMottakerInput(SERVICE_CODE, SERVICE_EDITION_CODE), null));
        input.setNotifikasjon(new NotifikasjonInput(notifikasjonLenke.toString(), notifikasjonMerkelapp.getBeskrivelse(), notifikasjonTekst));
        input.setMetadata(new MetadataInput(eksternId, grupperingsid, null, null, virksomhetsnummer));
        request.setNyOppgave(input);


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

    @Override
    public String lukkOppgave(String oppgaveId, OffsetDateTime tidspunkt) {

        var request = new OppgaveUtfoertMutationRequest();
        request.setId(oppgaveId);
        request.setUtfoertTidspunkt(tidspunkt.format(DateTimeFormatter.ISO_DATE_TIME));
        //request.setNyLenke("lenkeKvittering");

        var projection = new OppgaveUtfoertResultatResponseProjection().typename()
            .onOppgaveUtfoertVellykket(new OppgaveUtfoertVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.lukkOppgave(request, projection);
    }

    @Override
    public String lukkOppgaveByEksternId(String eksternId, Merkelapp merkelapp, OffsetDateTime tidspunkt) {

        var request = new OppgaveUtfoertByEksternId_V2MutationRequest();
        request.setEksternId(eksternId);
        request.setMerkelapp(merkelapp.getBeskrivelse());
        request.setUtfoertTidspunkt(tidspunkt.format(DateTimeFormatter.ISO_DATE_TIME));
        //request.setNyLenke("lenkeKvittering");

        var projection = new OppgaveUtfoertResultatResponseProjection().typename()
            .onOppgaveUtfoertVellykket(new OppgaveUtfoertVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.lukkOppgaveByEksternId(request, projection);
    }

    @Override
    public String ferdigstillSak(String id) {

        var request = new NyStatusSakMutationRequest();
        request.setId(id);
        request.setNyStatus(SaksStatus.FERDIG);

        var projection = new NyStatusSakResultatResponseProjection().typename()
            .onNyStatusSakVellykket(new NyStatusSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onKonflikt(new KonfliktResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());

        return klient.oppdaterSakStatus(request, projection);
    }

}
