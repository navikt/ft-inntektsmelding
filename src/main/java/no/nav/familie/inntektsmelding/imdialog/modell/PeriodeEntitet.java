package no.nav.familie.inntektsmelding.imdialog.modell;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import no.nav.vedtak.konfig.Tid;

import java.time.LocalDate;

@Embeddable
public class PeriodeEntitet {

    @Column(name = "fom")
    private LocalDate fom;

    @Column(name = "tom")
    private LocalDate tom;

    public PeriodeEntitet() {
        // Hibernate
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    private PeriodeEntitet(LocalDate fomDato, LocalDate tomDato) {
        if (fomDato == null) {
            throw new IllegalArgumentException("Fra og med dato må være satt.");
        }
        if (tomDato == null) {
            throw new IllegalArgumentException("Til og med dato må være satt.");
        }
        if (tomDato.isBefore(fomDato)) {
            throw new IllegalArgumentException("Til og med dato før fra og med dato.");
        }
        this.fom = fomDato;
        this.tom = tomDato;
    }

    public boolean overlapper(PeriodeEntitet other) {
        var fomBeforeOrEqual = this.getFom().isBefore(other.getTom()) || this.getFom().isEqual(other.getTom());
        var tomAfterOrEqual = this.getTom().isAfter(other.getFom()) || this.getTom().isEqual(other.getFom());
        return fomBeforeOrEqual && tomAfterOrEqual;
    }

    public static PeriodeEntitet fraOgMedTilOgMed(LocalDate fomDato, LocalDate tomDato) {
        return new PeriodeEntitet(fomDato, tomDato);
    }

    public static PeriodeEntitet fraOgMed(LocalDate fomDato) {
        return new PeriodeEntitet(fomDato, Tid.TIDENES_ENDE);
    }
}
