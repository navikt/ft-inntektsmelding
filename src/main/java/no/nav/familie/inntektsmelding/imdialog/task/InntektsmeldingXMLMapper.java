package no.nav.familie.inntektsmelding.imdialog.task;

import java.util.Map;

import jakarta.xml.bind.JAXBElement;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.ÅrsakTilInnsending;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ArbeidsgiverPrivat;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Avsendersystem;
import no.seres.xsd.nav.inntektsmelding_m._20181211.EndringIRefusjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.EndringIRefusjonsListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GjenopptakelseNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Inntekt;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Kontaktinformasjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory;
import no.seres.xsd.nav.inntektsmelding_m._20181211.OpphoerAvNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Refusjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Skjemainnhold;

public class InntektsmeldingXMLMapper {

    public static InntektsmeldingM map(InntektsmeldingEntitet inntektsmelding, Map<AktørIdEntitet, PersonIdent> aktørIdFnrMap) {
        var of = new ObjectFactory();
        Skjemainnhold skjemainnhold = new Skjemainnhold();

        if (OrganisasjonsnummerValidator.erGyldig(inntektsmelding.getArbeidsgiverIdent())) {
            var arbeidsgiver = new Arbeidsgiver();
            arbeidsgiver.setVirksomhetsnummer(inntektsmelding.getArbeidsgiverIdent());
            arbeidsgiver.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
            var agOrg = of.createSkjemainnholdArbeidsgiver(arbeidsgiver);
            skjemainnhold.setArbeidsgiver(agOrg);
        } else if (inntektsmelding.getArbeidsgiverIdent().length() == 13) {
            var arbeidsgiver = new ArbeidsgiverPrivat();
            var identArbeidsgiver = aktørIdFnrMap.get(new AktørIdEntitet(inntektsmelding.getArbeidsgiverIdent()));
            arbeidsgiver.setArbeidsgiverFnr(identArbeidsgiver.getIdent());
            arbeidsgiver.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
            var agPriv = of.createSkjemainnholdArbeidsgiverPrivat(arbeidsgiver);
            skjemainnhold.setArbeidsgiverPrivat(agPriv);
        }
        skjemainnhold.setArbeidsforhold(lagArbeidsforholdXml(inntektsmelding, of));
        skjemainnhold.setArbeidstakerFnr(aktørIdFnrMap.get(inntektsmelding.getAktørId()).getIdent());

        skjemainnhold.setAarsakTilInnsending(mapTilÅrsakTilInnsending(inntektsmelding.getÅrsakTilInnsending()));
        skjemainnhold.setAvsendersystem(lagAvsendersysem(inntektsmelding, of));

        skjemainnhold.setYtelse(mapTilYtelsetype(inntektsmelding.getYtelsetype()));
        mapYtelsespesifikkeFelter(skjemainnhold, of, inntektsmelding);
        skjemainnhold.setRefusjon(lagRefusjonXml(inntektsmelding, of));

        skjemainnhold.setOpphoerAvNaturalytelseListe(lagBortfaltNaturalytelse(inntektsmelding, of));
        skjemainnhold.setGjenopptakelseNaturalytelseListe(lagGjennopptattNaturalytelse(inntektsmelding, of));


        var imXml = new InntektsmeldingM();
        imXml.setSkjemainnhold(skjemainnhold);
        return imXml;
    }

    private static void mapYtelsespesifikkeFelter(Skjemainnhold skjemainnhold, ObjectFactory of, InntektsmeldingEntitet inntektsmelding) {
        switch (inntektsmelding.getYtelsetype()) {
            case FORELDREPENGER -> settFPStartdato(skjemainnhold, of, inntektsmelding);
            case PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OPPLÆRINGSPENGER -> {
                // Det er ingen ytelsespesifikke felter for disse ytelsene
            }
            // Følgende ytelser mangler implementasjon, må undersøke hva som skal settes for disse
            case SVANGERSKAPSPENGER, OMSORGSPENGER ->
                throw new IllegalStateException("Kan ikke mappe ytelsesspesifikke felter for ytelse " + inntektsmelding.getYtelsetype());
        }
    }

    private static void settFPStartdato(Skjemainnhold skjemainnhold, ObjectFactory of, InntektsmeldingEntitet inntektsmelding) {
        skjemainnhold.setStartdatoForeldrepengeperiode(of.createSkjemainnholdStartdatoForeldrepengeperiode(inntektsmelding.getStartDato()));
    }

    // TODO Vi bør ta en diskusjon på hva denne skal være
    private static Avsendersystem lagAvsendersysem(InntektsmeldingEntitet inntektsmelding, ObjectFactory of) {
        var as = new Avsendersystem();
        as.setSystemnavn("NAV_NO");
        as.setSystemversjon("1.0");
        as.setInnsendingstidspunkt(of.createAvsendersystemInnsendingstidspunkt(inntektsmelding.getOpprettetTidspunkt()));
        return as;
    }

    private static JAXBElement<GjenopptakelseNaturalytelseListe> lagGjennopptattNaturalytelse(InntektsmeldingEntitet inntektsmeldingEntitet,
                                                                                              ObjectFactory of) {
        var gjennoptakelseListeObjekt = new GjenopptakelseNaturalytelseListe();
        var gjennoptakelseListe = gjennoptakelseListeObjekt.getNaturalytelseDetaljer();
        inntektsmeldingEntitet.getBorfalteNaturalYtelser().stream().filter(by -> by.getPeriode().getTom().isBefore(Tid.TIDENES_ENDE)).forEach(tilkommetNat -> {
            var nd = new NaturalytelseDetaljer();
            nd.setFom(of.createNaturalytelseDetaljerFom(tilkommetNat.getPeriode().getFom()));
            nd.setBeloepPrMnd(of.createNaturalytelseDetaljerBeloepPrMnd(tilkommetNat.getMånedBeløp()));
            nd.setNaturalytelseType(of.createNaturalytelseDetaljerNaturalytelseType(mapTilNaturalytelsetype(tilkommetNat.getType())));
            gjennoptakelseListe.add(nd);
        });
        return of.createSkjemainnholdGjenopptakelseNaturalytelseListe(gjennoptakelseListeObjekt);
    }

    private static JAXBElement<OpphoerAvNaturalytelseListe> lagBortfaltNaturalytelse(InntektsmeldingEntitet inntektsmeldingEntitet,
                                                                                     ObjectFactory of) {
        var opphørListeObjekt = new OpphoerAvNaturalytelseListe();
        var opphørListe = opphørListeObjekt.getOpphoerAvNaturalytelse();
        inntektsmeldingEntitet.getBorfalteNaturalYtelser().stream().filter(by -> by.getPeriode().getTom().isEqual(Tid.TIDENES_ENDE)).forEach(nat -> {
            var nd = new NaturalytelseDetaljer();
            nd.setFom(of.createNaturalytelseDetaljerFom(nat.getPeriode().getFom()));
            nd.setBeloepPrMnd(of.createNaturalytelseDetaljerBeloepPrMnd(nat.getMånedBeløp()));
            nd.setNaturalytelseType(of.createNaturalytelseDetaljerNaturalytelseType(mapTilNaturalytelsetype(nat.getType())));
            opphørListe.add(nd);
        });
        return of.createSkjemainnholdOpphoerAvNaturalytelseListe(opphørListeObjekt);
    }

    private static JAXBElement<Refusjon> lagRefusjonXml(InntektsmeldingEntitet inntektsmeldingEntitet, ObjectFactory of) {
        var refusjon = new Refusjon();
        if (inntektsmeldingEntitet.getMånedRefusjon() != null) {
            refusjon.setRefusjonsbeloepPrMnd(of.createRefusjonRefusjonsbeloepPrMnd(inntektsmeldingEntitet.getMånedRefusjon()));
        }
        if (inntektsmeldingEntitet.getOpphørsdatoRefusjon() != null) {
            refusjon.setRefusjonsopphoersdato(of.createRefusjonRefusjonsopphoersdato(inntektsmeldingEntitet.getOpphørsdatoRefusjon()));
        }
        var endringListe = new EndringIRefusjonsListe();
        var liste = endringListe.getEndringIRefusjon();
        inntektsmeldingEntitet.getRefusjonsendringer().stream().map(rp -> {
            var endring = new EndringIRefusjon();
            endring.setEndringsdato(of.createEndringIRefusjonEndringsdato(rp.getFom()));
            endring.setRefusjonsbeloepPrMnd(of.createEndringIRefusjonRefusjonsbeloepPrMnd(rp.getRefusjonPrMnd()));
            return endring;
        }).forEach(liste::add);
        refusjon.setEndringIRefusjonListe(of.createRefusjonEndringIRefusjonListe(endringListe));
        return of.createSkjemainnholdRefusjon(refusjon);
    }

    private static JAXBElement<Arbeidsforhold> lagArbeidsforholdXml(InntektsmeldingEntitet inntektsmeldingEntitet, ObjectFactory of) {
        var arbeidsforhold = new Arbeidsforhold();

        // Inntekt
        var inntektBeløp = of.createInntektBeloep(inntektsmeldingEntitet.getMånedInntekt());
        var inntekt = new Inntekt();
        inntekt.setBeloep(inntektBeløp);
        // TODO Endringsårsak kan være enten "Tariffendring" eller "FeilInntekt", skal vi bruke disse?
        var inntektSkjemaVerdi = of.createArbeidsforholdBeregnetInntekt(inntekt);
        arbeidsforhold.setBeregnetInntekt(inntektSkjemaVerdi);

        // Startdato
        arbeidsforhold.setFoersteFravaersdag(of.createArbeidsforholdFoersteFravaersdag(inntektsmeldingEntitet.getStartDato()));
        return of.createSkjemainnholdArbeidsforhold(arbeidsforhold);
    }

    private static Kontaktinformasjon lagKontaktperson(InntektsmeldingEntitet inntektsmelding) {
        var kontaktPerson = inntektsmelding.getKontaktperson();
        var ki = new Kontaktinformasjon();
        ki.setTelefonnummer(kontaktPerson.getTelefonnummer());
        ki.setKontaktinformasjonNavn(kontaktPerson.getNavn());
        return ki;
    }

    private static String mapTilYtelsetype(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> "Foreldrepenger";
            case SVANGERSKAPSPENGER -> "Svangerskapspenger";
            case OPPLÆRINGSPENGER -> "Opplaeringspenger";
            case OMSORGSPENGER -> "Omsorgspenger";
            case PLEIEPENGER_SYKT_BARN -> "PleiepengerBarn";
            case PLEIEPENGER_NÆRSTÅENDE -> "PleiepengerNaerstaaende";
        };
    }

    private static String mapTilNaturalytelsetype(NaturalytelseType naturalytelsetype) {
        return switch (naturalytelsetype) {
            case ELEKTRISK_KOMMUNIKASJON -> "elektroniskKommunikasjon";
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> "aksjerGrunnfondsbevisTilUnderkurs";
            case LOSJI -> "losji";
            case KOST_DØGN -> "kostDoegn";
            case BESØKSREISER_HJEMMET_ANNET -> "besoeksreiserHjemmetAnnet";
            case KOSTBESPARELSE_I_HJEMMET -> "kostbesparelseIHjemmet";
            case RENTEFORDEL_LÅN -> "rentefordelLaan";
            case BIL -> "bil";
            case KOST_DAGER -> "kostDager";
            case BOLIG -> "bolig";
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> "skattepliktigDelForsikringer";
            case FRI_TRANSPORT -> "friTransport";
            case OPSJONER -> "opsjoner";
            case TILSKUDD_BARNEHAGEPLASS -> "tilskuddBarnehageplass";
            case ANNET -> "annet";
            case BEDRIFTSBARNEHAGEPLASS -> "bedriftsbarnehageplass";
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> "yrkebilTjenestligbehovKilometer";
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> "yrkebilTjenestligbehovListepris";
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> "innbetalingTilUtenlandskPensjonsordning";
        };
    }

    private static String mapTilÅrsakTilInnsending(ÅrsakTilInnsending årsakTilInnsending) {
        return switch (årsakTilInnsending) {
            case NY -> "Ny";
            case ENDRING -> "Endring";
        };
    }
}
