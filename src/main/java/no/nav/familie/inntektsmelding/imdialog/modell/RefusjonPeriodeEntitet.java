package no.nav.familie.inntektsmelding.imdialog.modell;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "RefusjonPeriodeEntitet")
@Table(name = "REFUSJON_PERIODE")
public class RefusjonPeriodeEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REFUSJON_PERIODE")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Embedded
    private PeriodeEntitet periode;

    @Column(name = "beloep")
    private BigDecimal beløp;

    public RefusjonPeriodeEntitet() {
        // Hibernate
    }

    public RefusjonPeriodeEntitet(LocalDate fom, LocalDate tom, BigDecimal beløp) {
        this.periode = tom == null ? PeriodeEntitet.fraOgMed(fom) : PeriodeEntitet.fraOgMedTilOgMed(fom, tom);
        this.beløp = beløp;
    }

    void setInntektsmelding(InntektsmeldingEntitet inntektsmeldingEntitet) {
        this.inntektsmelding = inntektsmeldingEntitet;
    }

    public PeriodeEntitet getPeriode() {
        return periode;
    }

    public BigDecimal getBeløp() {
        return beløp;
    }

    @Override
    public String toString() {
        return "RefusjonPeriodeEntitet{" + "periode=" + periode + ", beløp=" + beløp + '}';
    }
}
