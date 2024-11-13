package no.nav.familie.inntektsmelding.pip;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnAutoriseringKlient;

@Dependent
public class AltinnTilgangTjeneste {

    private final AltinnAutoriseringKlient altinnKlient;

    public AltinnTilgangTjeneste() {
        this.altinnKlient = AltinnAutoriseringKlient.instance();
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        return !altinnKlient.harTilgangTilBedriften(orgNr);
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }
}
