package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoForLister;
import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoMedNavnPåUkedag;
import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoNorsk;
import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoOgTidNorsk;
import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterPersonnummer;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingPdfDataMapperTest {
    private final String FORNAVN = "Test";
    private final String MELLOMNAVN = "Tester";
    private final String ETTERNAVN = "Testesen";
    private final String ARBEIDSGIVERIDENT = "999999999";
    private final String ARBEIDSGIVERNAVN = "Arbeidsgvier 1";
    private final String NAVN = "Kontaktperson navn";
    private final String ORGNUMMER = "999999999";
    private final BigDecimal REFUSJONSBELØP = BigDecimal.valueOf(35000);
    private final AktørIdEntitet AKTØRID_SØKER = new AktørIdEntitet("1234567891234");
    private final BigDecimal INNTEKT = BigDecimal.valueOf(40000);
    private final LocalDateTime OPPRETTETTIDSPUNKT = LocalDateTime.now();
    private final LocalDate START_DATO = LocalDate.now();
    private PersonInfo personInfo;
    private PersonIdent personIdent;

    @BeforeEach
    public void setUP() {
        personIdent = new PersonIdent("11111111111");
        personInfo = new PersonInfo(FORNAVN, MELLOMNAVN, ETTERNAVN, personIdent, AKTØRID_SØKER, LocalDate.now(), null);
    }

    @Test
    public void skal_opprette_pdfData() {

        var naturalytelseFraDato = LocalDate.of(2024, 6, 10);
        var naturalytelseBeløp = BigDecimal.valueOf(2000);
        var naturalytelse = BortaltNaturalytelseEntitet.builder()
            .medPeriode(naturalytelseFraDato, Tid.TIDENES_ENDE)
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(naturalytelseBeløp)
            .build();

        var inntektsmeldingEntitet = lagStandardInntektsmeldingBuilder()
            .medBortfaltNaturalytelser(List.of(naturalytelse))
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingData(inntektsmeldingEntitet, ARBEIDSGIVERNAVN, personInfo, ARBEIDSGIVERIDENT);

        assertThat(pdfData.getArbeidsgiverIdent()).isEqualTo(ARBEIDSGIVERIDENT);
        assertThat(pdfData.getAvsenderSystem()).isEqualTo("NAV_NO");
        assertThat(pdfData.getArbeidsgiverNavn()).isEqualTo(ARBEIDSGIVERNAVN);
        assertThat(pdfData.getKontaktperson().navn()).isEqualTo(NAVN);
        assertThat(pdfData.getKontaktperson().telefonnummer()).isEqualTo(ORGNUMMER);
        assertThat(pdfData.getMånedInntekt()).isEqualTo(INNTEKT);
        assertThat(pdfData.getNavnSøker()).isEqualTo(ETTERNAVN + " " + FORNAVN + " " + MELLOMNAVN);
        assertThat(pdfData.getFornavnSøker()).isEqualTo(FORNAVN);
        assertThat(pdfData.getYtelsetype()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(pdfData.getOpprettetTidspunkt()).isEqualTo(formaterDatoOgTidNorsk(OPPRETTETTIDSPUNKT));
        assertThat(pdfData.getStartDato()).isEqualTo(formaterDatoMedNavnPåUkedag(START_DATO));
        assertThat(pdfData.getPersonnummer()).isEqualTo(formaterPersonnummer(personIdent.getIdent()));
        assertThat(pdfData.getRefusjonsbeløp()).isEqualTo(REFUSJONSBELØP);
        assertThat(pdfData.getRefusjonsendringer()).isEmpty();
        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isTrue();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isFalse();
        assertThat(pdfData.getNaturalytelser().getFirst().fom()).isEqualTo(formaterDatoForLister(naturalytelseFraDato));
        assertThat(pdfData.getNaturalytelser().getFirst().beloep()).isEqualTo(naturalytelseBeløp);
        assertThat(pdfData.getNaturalytelser().getFirst().naturalytelseType()).isEqualTo("Aksjer grunnfondsbevis til underkurs");
    }

    @Test
    public void skal_mappe_flere_tilkommet_naturalytelser_av_samme_type_korrekt() {
        var naturalytelseFraDato = LocalDate.of(2024, 6, 1);
        var naturalytelseTilDato = LocalDate.of(2024, 7, 1);
        var naturalytelseAndreFraDato = naturalytelseTilDato.plusWeeks(1);
        var naturalytelseAndreTilDato = naturalytelseTilDato.plusWeeks(4);
        var naturalytelseTredjeTilDato = naturalytelseTilDato.plusWeeks(6);
        var naturalytelseBeløp = BigDecimal.valueOf(1000);

        var naturalytelser = List.of(BortaltNaturalytelseEntitet.builder()
            .medPeriode(naturalytelseFraDato, naturalytelseTilDato)
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(naturalytelseBeløp)
            .build(), BortaltNaturalytelseEntitet.builder()
            .medPeriode(naturalytelseAndreFraDato, naturalytelseAndreTilDato)
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(naturalytelseBeløp)
            .build(), BortaltNaturalytelseEntitet.builder()
            .medPeriode(naturalytelseTredjeTilDato, Tid.TIDENES_ENDE)
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(naturalytelseBeløp)
            .build());

        var inntektsmeldingEntitet = lagStandardInntektsmeldingBuilder()
            .medBortfaltNaturalytelser(naturalytelser)
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingData(inntektsmeldingEntitet, ARBEIDSGIVERNAVN, personInfo, ARBEIDSGIVERIDENT);

        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isFalse();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isFalse();
        assertThat(pdfData.getNaturalytelser()).hasSize(5);

        var bortfalteNaturalytelser = pdfData.getNaturalytelser().stream().filter(NaturalYtelse::erBortfalt).toList();

        assertThat(bortfalteNaturalytelser).hasSize(3);

        var forventetFørsteFraDato = naturalytelseTilDato.plusDays(1);
        var forventetAndreFraDato = naturalytelseAndreTilDato.plusDays(1);


        var tilkomneNaturalytelser = pdfData.getNaturalytelser().stream().filter(naturalytelse -> !naturalytelse.erBortfalt()).toList();

        assertThat(tilkomneNaturalytelser).hasSize(2);
        assertThat(tilkomneNaturalytelser.getFirst().fom()).isEqualTo(formaterDatoForLister(forventetFørsteFraDato));
        assertThat(tilkomneNaturalytelser.get(1).fom()).isEqualTo(formaterDatoForLister(forventetAndreFraDato));
    }

    @Test
    public void skal_mappe_naturalytelser_av_ulik_type_korrekt() {
        var naturalytelseFraDato = LocalDate.of(2024, 6, 1);
        var naturalytelseTilDato = LocalDate.of(2024, 7, 1);
        var naturalytelseAndreFraDato = naturalytelseTilDato.plusWeeks(1);
        var naturalytelseAndreTilDato = naturalytelseTilDato.plusWeeks(4);
        var naturalytelseTredjeTilDato = naturalytelseTilDato.plusWeeks(6);
        var naturalytelseBeløp = BigDecimal.valueOf(1000);

        var naturalytelser = List.of(BortaltNaturalytelseEntitet.builder()
            .medPeriode(naturalytelseFraDato, naturalytelseTilDato)
            .medType(NaturalytelseType.BIL)
            .medMånedBeløp(naturalytelseBeløp)
            .build(), BortaltNaturalytelseEntitet.builder()
            .medPeriode(naturalytelseAndreFraDato, naturalytelseAndreTilDato)
            .medType(NaturalytelseType.BOLIG)
            .medMånedBeløp(naturalytelseBeløp)
            .build(), BortaltNaturalytelseEntitet.builder()
            .medPeriode(naturalytelseTredjeTilDato, Tid.TIDENES_ENDE)
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(naturalytelseBeløp)
            .build());

        var inntektsmeldingEntitet = lagStandardInntektsmeldingBuilder()
            .medBortfaltNaturalytelser(naturalytelser)
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingData(inntektsmeldingEntitet, ARBEIDSGIVERNAVN, personInfo, ARBEIDSGIVERIDENT);

        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isFalse();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isFalse();
        assertThat(pdfData.getNaturalytelser()).hasSize(5);

        var bortfalteNaturalytelser = pdfData.getNaturalytelser().stream().filter(NaturalYtelse::erBortfalt).toList();
        assertThat(bortfalteNaturalytelser).hasSize(3);

        var forventetFørsteFraDato = naturalytelseTilDato.plusDays(1);
        var forventetAndreFraDato = naturalytelseAndreTilDato.plusDays(1);

        var tilkomneNaturalytelser = pdfData.getNaturalytelser().stream().filter(naturalytelse -> !naturalytelse.erBortfalt()).toList();

        assertThat(tilkomneNaturalytelser).hasSize(2);
        assertThat(tilkomneNaturalytelser.getFirst().fom()).isEqualTo(formaterDatoForLister(forventetFørsteFraDato));
        assertThat(tilkomneNaturalytelser.getFirst().naturalytelseType()).isEqualTo("Bil");
        assertThat(tilkomneNaturalytelser.get(1).fom()).isEqualTo(formaterDatoForLister(forventetAndreFraDato));
        assertThat(tilkomneNaturalytelser.get(1).naturalytelseType()).isEqualTo("Bolig");
    }

    @Test
    public void skal_mappe_pdfData_uten_naturalytser() {

        var inntektsmeldingEntitet = lagStandardInntektsmeldingBuilder()
            .medBortfaltNaturalytelser(Collections.emptyList())
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingData(inntektsmeldingEntitet, ARBEIDSGIVERNAVN, personInfo, ARBEIDSGIVERIDENT);

        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isTrue();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isTrue();
        assertThat(pdfData.getNaturalytelser()).isEmpty();
    }

    private InntektsmeldingEntitet.Builder lagStandardInntektsmeldingBuilder() {
        return InntektsmeldingEntitet.builder()
            .medAktørId(AKTØRID_SØKER)
            .medKontaktperson(new KontaktpersonEntitet(NAVN, ORGNUMMER))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(INNTEKT)
            .medStartDato(START_DATO)
            .medMånedRefusjon(REFUSJONSBELØP)
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medOpprettetTidspunkt(OPPRETTETTIDSPUNKT)
            .medArbeidsgiverIdent(ARBEIDSGIVERIDENT);
    }
}
