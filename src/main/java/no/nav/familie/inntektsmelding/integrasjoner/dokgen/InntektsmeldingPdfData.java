package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InntektsmeldingPdfData {
    private String avsenderSystem;
    private String navnSøker;
    private String fornavnSøker;
    private String personnummer;
    private Ytelsetype ytelsetype;
    private String arbeidsgiverIdent;
    private String arbeidsgiverNavn;
    private Kontaktperson kontaktperson;
    private String startDato;
    private BigDecimal månedInntekt;
    private String opprettetTidspunkt;
    private BigDecimal refusjonsbeløp;
    private String refusjonOpphørsdato;
    private List<RefusjonPeriode> endringIrefusjonsperioder = new ArrayList<>();
    private List<NaturalYtelse> naturalytelser = new ArrayList<>();
    private boolean ingenBortfaltNaturalytelse;
    private boolean ingenGjenopptattNaturalytelse;

    public String getAvsenderSystem() {
        return avsenderSystem;
    }

    public String getNavnSøker() {
        return navnSøker;
    }

    public String getPersonnummer() {
        return personnummer;
    }

    public Ytelsetype getYtelsetype() {
        return ytelsetype;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public Kontaktperson getKontaktperson() {
        return kontaktperson;
    }

    public String getStartDato() {
        return startDato;
    }

    public BigDecimal getMånedInntekt() {
        return månedInntekt;
    }

    public String getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public String getRefusjonOpphørsdato() {
        return refusjonOpphørsdato;
    }

    public List<RefusjonPeriode> getEndringIrefusjonsperioder() {
        return endringIrefusjonsperioder;
    }

    public List<NaturalYtelse> getNaturalytelser() {
        return naturalytelser;
    }

    public BigDecimal getRefusjonsbeløp() {
        return refusjonsbeløp;
    }

    public boolean ingenGjenopptattNaturalytelse() {
        return ingenGjenopptattNaturalytelse;
    }

    public boolean ingenBortfaltNaturalytelse() {
        return ingenBortfaltNaturalytelse;
    }

    public String getFornavnSøker() {
        return fornavnSøker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InntektsmeldingPdfData that = (InntektsmeldingPdfData) o;
        return ingenBortfaltNaturalytelse == that.ingenBortfaltNaturalytelse && ingenGjenopptattNaturalytelse == that.ingenGjenopptattNaturalytelse
            && Objects.equals(avsenderSystem, that.avsenderSystem) && Objects.equals(navnSøker, that.navnSøker) && Objects.equals(fornavnSøker,
            that.fornavnSøker) && Objects.equals(personnummer, that.personnummer) && ytelsetype == that.ytelsetype
            && Objects.equals(arbeidsgiverIdent,
            that.arbeidsgiverIdent) && Objects.equals(arbeidsgiverNavn, that.arbeidsgiverNavn) && Objects.equals(kontaktperson, that.kontaktperson)
            && Objects.equals(startDato, that.startDato) && Objects.equals(månedInntekt, that.månedInntekt) && Objects.equals(opprettetTidspunkt,
            that.opprettetTidspunkt) && Objects.equals(refusjonsbeløp, that.refusjonsbeløp) && Objects.equals(refusjonOpphørsdato,
            that.refusjonOpphørsdato) && Objects.equals(endringIrefusjonsperioder, that.endringIrefusjonsperioder) && Objects.equals(naturalytelser,
            that.naturalytelser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avsenderSystem, navnSøker, fornavnSøker, personnummer, ytelsetype, arbeidsgiverIdent, arbeidsgiverNavn, kontaktperson,
            startDato, månedInntekt, opprettetTidspunkt, refusjonsbeløp, refusjonOpphørsdato, endringIrefusjonsperioder, naturalytelser,
            ingenBortfaltNaturalytelse, ingenGjenopptattNaturalytelse);
    }

    public static String formaterPersonnummer(String personnummer) {
        if (personnummer != null && personnummer.length() == 11) {
            var formatertPersonnummer = new StringBuilder(personnummer);
            formatertPersonnummer.insert(6, " ");
            return formatertPersonnummer.toString();
        }
        return personnummer;
    }

    public static String formaterDatoNorsk(LocalDate dato) {
        if (dato == null) {
            return null;
        }
        return dato.format(ofPattern("d. MMMM yyyy", Locale.forLanguageTag("NO")));
    }

    public static String formaterDatoMedNavnPåUkedag(LocalDate dato) {
        if (dato == null) {
            return null;
        }
        var navnPåUkedag = dato.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("NO"));
        navnPåUkedag = navnPåUkedag.substring(0,1).toUpperCase() + navnPåUkedag.substring(1);
        return String.format(navnPåUkedag + " " + dato.format(ofPattern("d. MMMM yyyy", Locale.forLanguageTag("NO"))));
    }

    public static String formaterDatoOgTidNorsk(LocalDateTime opprettetTidspunkt) {
        if (opprettetTidspunkt == null) {
            return null;
        }
        return opprettetTidspunkt.format(ofPattern("d. MMMM yyyy HH:mm:ss", Locale.forLanguageTag("NO")));
    }

    public void anonymiser() {
        this.personnummer = personnummer.substring(0, 4) + "** *****";
        this.arbeidsgiverIdent = arbeidsgiverIdent.substring(0, 4) + "** *****";
    }

    public static class Builder {
        private InntektsmeldingPdfData kladd;

        public Builder() {
            kladd = new InntektsmeldingPdfData();
        }

        public Builder medAvsenderSystem(String avsenderSystem) {
            this.kladd.avsenderSystem = avsenderSystem;
            return this;
        }

        public Builder medNavn(String navn) {
            this.kladd.navnSøker = navn;
            return this;
        }

        public Builder medForNavnSøker(String fornavn) {
            this.kladd.fornavnSøker = fornavn;
            return this;
        }

        public Builder medPersonnummer(String personnummer) {
            this.kladd.personnummer = formaterPersonnummer(personnummer);
            return this;
        }

        public Builder medYtelseNavn(Ytelsetype ytelsenavn) {
            this.kladd.ytelsetype = ytelsenavn;
            return this;
        }

        public Builder medArbeidsgiverIdent(String arbeidsgiverIdent) {
            this.kladd.arbeidsgiverIdent = arbeidsgiverIdent;
            return this;
        }

        public Builder medArbeidsgiverNavn(String arbeidsgiverNavn) {
            this.kladd.arbeidsgiverNavn = arbeidsgiverNavn;
            return this;
        }

        public Builder medStartDato(LocalDate startDato) {
            this.kladd.startDato = formaterDatoMedNavnPåUkedag(startDato);
            return this;
        }

        public Builder medMånedInntekt(BigDecimal månedInntekt) {
            this.kladd.månedInntekt = månedInntekt;
            return this;
        }

        public Builder medOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            this.kladd.opprettetTidspunkt = formaterDatoOgTidNorsk(opprettetTidspunkt);
            return this;
        }

        public Builder medRefusjonsbeløp(BigDecimal refusjonsbeløp) {
            this.kladd.refusjonsbeløp = refusjonsbeløp;
            return this;
        }

        public Builder medRefusjonOpphørsdato(LocalDate refusjonOpphørsdato) {
            this.kladd.refusjonOpphørsdato = formaterDatoNorsk(refusjonOpphørsdato);
            return this;
        }

        public Builder medEndringIRefusjonsperioder(List<RefusjonPeriode> refusjonsperioder) {
            this.kladd.endringIrefusjonsperioder = refusjonsperioder;
            return this;
        }

        public Builder medNaturalytelser(List<NaturalYtelse> naturalYtelser) {
            this.kladd.naturalytelser = naturalYtelser;
            return this;
        }

        public Builder medIngenBortfaltNaturalytelse(boolean ingenBortfalt) {
            this.kladd.ingenBortfaltNaturalytelse = ingenBortfalt;
            return this;
        }

        public Builder medIngenGjenopptattNaturalytelse(boolean ingenGjennopptatt) {
            this.kladd.ingenGjenopptattNaturalytelse = ingenGjennopptatt;
            return this;
        }

        public Builder medKontaktperson(Kontaktperson kontaktperson) {
            this.kladd.kontaktperson = kontaktperson;
            return this;
        }

        public InntektsmeldingPdfData build() {
            return kladd;
        }
    }
}
