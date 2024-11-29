package no.nav.familie.inntektsmelding.koder;

/**
 * Hvorfor inntekt i inntektsmeldingen er endret fra snittet de siste tre måneder
 */
public enum Endringsårsak {
    PERMITTERING("Permittert"),
    NY_STILLING("Ny stilling"),
    NY_STILLINGSPROSENT("Ny stillingsprosent"),
    SYKEFRAVÆR("Sykefravær"),
    BONUS("Bonus"),
    FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER("Ferietrekk eller utbetaling av feriepenger"),
    NYANSATT("Nyansatt"),
    MANGELFULL_RAPPORTERING_AORDNING("Mangelfull rapportering i a-ordning"),
    INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING("Ikke rapportert i a-ordning"),
    TARIFFENDRING("Tariffendring"),
    FERIE("Ferie"),
    VARIG_LØNNSENDRING("Varig lønnsendring"),
    PERMISJON("Permisjon");

    private final String beskrivelse;

    Endringsårsak(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}

