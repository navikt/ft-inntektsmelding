package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

public enum Merkelapp {
    INNTEKTSMELDING_FP("Inntektsmelding foreldrepenger"),
    INNTEKTSMELDING_SVP("Inntektsmelding svangerskapspenger"),
    INNTEKTSMELDING_PSB("Inntektsmelding pleiepenger sykt barn"),
    INNTEKTSMELDING_OMP("Inntektsmelding omsorgspenger"),
    INNTEKTSMELDING_PILS("Inntektsmelding pleiepenger i livets sluttfase"),
    INNTEKTSMELDING_OPP("Inntektsmelding opplæringspenger");

    private final String beskrivelse;

    Merkelapp(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}


