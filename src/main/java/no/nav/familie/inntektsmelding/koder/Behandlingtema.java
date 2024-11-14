package no.nav.familie.inntektsmelding.koder;

public enum Behandlingtema {

    FORELDREPENGER("ab0326"),
    SVANGERSKAPSPENGER("ab0126"),
    PLEIEPENGER_SYKT_BARN("ab0320"),
    PLEIEPENGER_LIVETS_SLUTTFASE("ab0094"),
    OMSORGSPENGER("ab0149");

    private final String offisiellKode;

    Behandlingtema(String offisiellKode) {
        this.offisiellKode = offisiellKode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }
}
