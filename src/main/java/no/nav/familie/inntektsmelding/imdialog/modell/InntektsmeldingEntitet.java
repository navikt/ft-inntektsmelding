package no.nav.familie.inntektsmelding.imdialog.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Entity(name = "InntektsmeldingEntitet")
@Table(name = "INNTEKTSMELDING")
public class InntektsmeldingEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNTEKTSMELDING")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "bruker_aktoer_id", nullable = false, updatable = false)))
    private AktørIdEntitet aktørId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ytelse_type")
    private Ytelsetype ytelsetype;

    @Column(name = "arbeidsgiver_ident")
    private String arbeidsgiverIdent;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "inntektsmelding")
    private KontaktpersonEntitet kontaktperson;

    @Column(name = "start_dato_permisjon")
    private LocalDate startDato;

    @Column(name = "maaned_inntekt")
    private BigDecimal månedInntekt;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "inntektsmelding")
    private List<RefusjonPeriodeEntitet> refusjonsPeriode = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "inntektsmelding")
    private List<NaturalytelseEntitet> naturalYtelse = new ArrayList<>();

    public InntektsmeldingEntitet() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public AktørIdEntitet getAktørId() {
        return aktørId;
    }

    public Ytelsetype getYtelsetype() {
        return ytelsetype;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public LocalDate getStartDato() {
        return startDato;
    }

    public BigDecimal getMånedInntekt() {
        return månedInntekt;
    }

    public List<RefusjonPeriodeEntitet> getRefusjonsPerioder() {
        return refusjonsPeriode;
    }

    public List<NaturalytelseEntitet> getNaturalYtelser() {
        return naturalYtelse;
    }

    public KontaktpersonEntitet getKontaktperson() {
        return kontaktperson;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    void leggTilRefusjonsperiode(RefusjonPeriodeEntitet refusjonPeriodeEntitet) {
        var finnesOverlapp = refusjonsPeriode.stream().anyMatch(rp -> rp.getPeriode().overlapper(refusjonPeriodeEntitet.getPeriode()));
        if (finnesOverlapp) {
            var msg = String.format("Overlapp mellom ny refusjonsperiode %s og allerede lagt til perioder %s", refusjonPeriodeEntitet,
                refusjonsPeriode);
            throw new IllegalArgumentException(msg);
        }
        refusjonPeriodeEntitet.setInntektsmelding(this);
        refusjonsPeriode.add(refusjonPeriodeEntitet);
    }

    void leggTilNaturalytelse(NaturalytelseEntitet naturalytelseEntitet) {
        naturalytelseEntitet.setInntektsmelding(this);
        if (Boolean.TRUE.equals(naturalytelseEntitet.getErBortfalt()) && naturalytelseEntitet.getBeløp().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Bortfalt naturalytelse på kr 0 " + naturalytelseEntitet);
        }
        naturalYtelse.add(naturalytelseEntitet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InntektsmeldingEntitet entitet = (InntektsmeldingEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) && ytelsetype == entitet.ytelsetype && Objects.equals(arbeidsgiverIdent,
            entitet.arbeidsgiverIdent) && Objects.equals(startDato, entitet.startDato) && Objects.equals(opprettetTidspunkt,
            entitet.opprettetTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, ytelsetype, arbeidsgiverIdent, startDato, opprettetTidspunkt);
    }

    @Override
    public String toString() {
        return "InntektsmeldingEntitet{" + "id=" + id + ", aktørId=" + aktørId + ", ytelsetype=" + ytelsetype + ", arbeidsgiverIdent='"
            + arbeidsgiverIdent + '\'' + ", startDato=" + startDato + ", månedInntekt=" + månedInntekt + ", opprettetTidspunkt=" + opprettetTidspunkt
            + ", refusjonsPeriode=" + refusjonsPeriode + ", naturalYtelse=" + naturalYtelse + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final InntektsmeldingEntitet kladd = new InntektsmeldingEntitet();

        public Builder medAktørId(AktørIdEntitet aktørId) {
            kladd.aktørId = aktørId;
            return this;
        }

        // Mulighet for å eksplisitt overstyre opprettet tidspunkt for bruk i test
        public Builder medOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            kladd.opprettetTidspunkt = opprettetTidspunkt;
            return this;
        }

        public Builder medYtelsetype(Ytelsetype ytelsetype) {
            kladd.ytelsetype = ytelsetype;
            return this;
        }

        public Builder medArbeidsgiverIdent(String arbeidsgiverIdent) {
            kladd.arbeidsgiverIdent = arbeidsgiverIdent;
            return this;
        }

        public Builder medKontaktperson(KontaktpersonEntitet kontaktpersonEntitet) {
            kontaktpersonEntitet.setInntektsmelding(kladd);
            kladd.kontaktperson = kontaktpersonEntitet;
            return this;
        }

        public Builder medStartDato(LocalDate startDato) {
            kladd.startDato = startDato;
            return this;
        }

        public Builder medMånedInntekt(BigDecimal månedInntekt) {
            kladd.månedInntekt = månedInntekt;
            return this;
        }

        public Builder medRefusjonsPeriode(List<RefusjonPeriodeEntitet> refusjonsPeriode) {
            refusjonsPeriode.forEach(kladd::leggTilRefusjonsperiode);
            return this;
        }

        public Builder medNaturalYtelse(List<NaturalytelseEntitet> naturalYtelse) {
            naturalYtelse.forEach(kladd::leggTilNaturalytelse);
            return this;
        }

        public InntektsmeldingEntitet build() {
            return kladd;
        }

    }
}
