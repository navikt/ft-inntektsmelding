package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoForLister;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.utils.mapper.NaturalYtelseMapper;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingPdfDataMapper {
    public static InntektsmeldingPdfData mapInntektsmeldingData(InntektsmeldingEntitet inntektsmelding,
                                                                String arbeidsgiverNavn,
                                                                PersonInfo personInfo,
                                                                String arbeidsgvierIdent) {
        var imDokumentdataBuilder = new InntektsmeldingPdfData.Builder()
            .medNavn(personInfo.mapNavn())
            .medForNavnSøker(personInfo.mapFornavn())
            .medPersonnummer(personInfo.fødselsnummer().getIdent())
            .medArbeidsgiverIdent(arbeidsgvierIdent)
            .medArbeidsgiverNavn(arbeidsgiverNavn)
            .medAvsenderSystem("NAV_NO")
            .medYtelseNavn(inntektsmelding.getYtelsetype())
            .medOpprettetTidspunkt(inntektsmelding.getOpprettetTidspunkt())
            .medStartDato(inntektsmelding.getStartDato())
            .medMånedInntekt(inntektsmelding.getMånedInntekt())
            .medKontaktperson(mapKontaktperson(inntektsmelding))
            .medNaturalytelser(mapNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medIngenBortfaltNaturalytelse(erIngenBortalteNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medIngenGjenopptattNaturalytelse(erIngenGjenopptatteNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medEndringsårsaker(mapEndringsårsaker(inntektsmelding.getEndringsårsaker()));

        if (inntektsmelding.getMånedRefusjon() != null) {
            imDokumentdataBuilder.medRefusjonsbeløp(inntektsmelding.getMånedRefusjon());
        }

        var opphørsdato = inntektsmelding.getOpphørsdatoRefusjon() != null ? inntektsmelding.getOpphørsdatoRefusjon() : null;
        imDokumentdataBuilder.medRefusjonsendringer(mapRefusjonsendringPerioder(inntektsmelding.getRefusjonsendringer(), opphørsdato));

        return imDokumentdataBuilder.build();
    }

    private static List<Endringsarsak> mapEndringsårsaker(List<EndringsårsakEntitet> endringsårsaker) {
        return endringsårsaker.stream()
            .map( endringsårsakEntitet -> new Endringsarsak(endringsårsakEntitet.getÅrsak().getBeskrivelse(), formaterDatoForLister(endringsårsakEntitet.getFom().orElse(null)), formaterDatoForLister(endringsårsakEntitet.getTom().orElse(null)),
                formaterDatoForLister(endringsårsakEntitet.getBleKjentFom().orElse(null))))
            .toList();
    }

    private static Kontaktperson mapKontaktperson(InntektsmeldingEntitet inntektsmelding) {
        if (Kildesystem.FPSAK.equals(inntektsmelding.getKildesystem())) {
            return new Kontaktperson(inntektsmelding.getOpprettetAv(), inntektsmelding.getOpprettetAv());
        } else {
            return new Kontaktperson(inntektsmelding.getKontaktperson().getNavn(), inntektsmelding.getKontaktperson().getTelefonnummer());
        }
    }

    private static boolean erIngenGjenopptatteNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalYtelser) {
        return naturalYtelser.isEmpty() || naturalYtelser.stream().filter(n -> n.getPeriode().getTom().isBefore(Tid.TIDENES_ENDE)).toList().isEmpty();
    }

    private static boolean erIngenBortalteNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalYtelser) {
        return naturalYtelser.isEmpty();
    }

    private static List<NaturalYtelse> mapNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalytelser) {
        return NaturalYtelseMapper.mapNaturalYtelser(naturalytelser).stream()
            .map(InntektsmeldingPdfDataMapper::opprettNaturalytelserTilBrev)
            .toList();
    }

    private static NaturalYtelse opprettNaturalytelserTilBrev(NaturalYtelseMapper.NaturalYtelse bn) {
        return new NaturalYtelse(formaterDatoForLister(bn.fom()),
            mapTypeTekst(bn.type()),
            bn.beløp(),
            bn.bortfallt());
    }

    private static String mapTypeTekst(NaturalytelseType type) {
        return switch (type) {
            case ELEKTRISK_KOMMUNIKASJON -> "Elektrisk kommunikasjon";
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> "Aksjer grunnfondsbevis til underkurs";
            case LOSJI -> "Losji";
            case KOST_DØGN -> "Kostpenger døgnsats";
            case BESØKSREISER_HJEMMET_ANNET -> "Besøksreiser hjemmet annet";
            case KOSTBESPARELSE_I_HJEMMET -> "Kostbesparelser i hjemmet";
            case RENTEFORDEL_LÅN -> "Rentefordel lån";
            case BIL -> "Bil";
            case KOST_DAGER -> "Kostpenger dager";
            case BOLIG -> "Bolig";
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> "Skattepliktig del forsikringer";
            case FRI_TRANSPORT -> "Fri transport";
            case OPSJONER -> "Opsjoner";
            case TILSKUDD_BARNEHAGEPLASS -> "Tilskudd barnehageplass";
            case ANNET -> "Annet";
            case BEDRIFTSBARNEHAGEPLASS -> "Bedriftsbarnehageplass";
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> "Yrkesbil tjenesteligbehov kilometer";
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> "Yrkesbil tjenesteligbehov listepris";
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> "Innbetaling utenlandsk pensjonsordning";
        };
    }

    private static List<RefusjonsendringPeriode> mapRefusjonsendringPerioder(List<RefusjonsendringEntitet> refusjonsendringer, LocalDate opphørsdato) {
        var refusjonsendringerTilBrev = refusjonsendringer.stream()
            .map(rpe -> new RefusjonsendringPeriode(formaterDatoForLister(rpe.getFom()),
                rpe.getRefusjonPrMnd()))
            .collect(Collectors.toList());
        if (opphørsdato != null && !opphørsdato.equals(Tid.TIDENES_ENDE)) {
            refusjonsendringerTilBrev.add(new RefusjonsendringPeriode(formaterDatoForLister(opphørsdato), BigDecimal.ZERO));
        }
        return refusjonsendringerTilBrev;
    }
}
