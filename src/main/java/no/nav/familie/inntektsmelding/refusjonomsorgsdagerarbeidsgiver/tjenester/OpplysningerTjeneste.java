package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.InnsenderDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.OpplysningerResponseDto;

@ApplicationScoped
public class OpplysningerTjeneste {
    private PersonTjeneste personTjeneste;

    public OpplysningerTjeneste() {
        // CDI
    }

    public OpplysningerTjeneste(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    public OpplysningerResponseDto hentOpplysninger() {
        var innsender = personTjeneste.hentInnloggetPerson(Ytelsetype.OMSORGSPENGER);

        if (innsender == null) {
            return new OpplysningerResponseDto(null);
        }

        return new OpplysningerResponseDto(
                new InnsenderDto(innsender.fornavn(), innsender.mellomnavn(), innsender.etternavn(),
                        innsender.telefonnummer()));
    }
}
