package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

public class FantIkkeArbeidstakerException extends SlåOppArbeidstakerException {

    public FantIkkeArbeidstakerException() {
        super("Fant ikke arbeidstaker");
    }
}
