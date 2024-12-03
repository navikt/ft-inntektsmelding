package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

public class InnsenderHarIkkeTilgangTilArbeidsforholdException extends SlåOppArbeidstakerException {

    public InnsenderHarIkkeTilgangTilArbeidsforholdException() {
        super("Innsender har ikke tilgang til noen av arbeidsforholdene til arbeidstaker");
    }
}
