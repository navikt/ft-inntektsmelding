package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.SlåOppArbeidstakerResponseDto;

@ApplicationScoped
public class ArbeidstakerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidstakerTjeneste.class);
    private PersonTjeneste personTjeneste;

    public ArbeidstakerTjeneste() {
        // CDI
    }

    public ArbeidstakerTjeneste(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    public SlåOppArbeidstakerResponseDto slåOppArbeidstaker(PersonIdent ident) {
        var personInfo = personTjeneste.hentPersonFraIdent(ident, Ytelsetype.OMSORGSDAGER);
        // TODO: Hent arbeidsforhold for personen fra AAReg
        // TODO: Sjekk tilganger til å hente arbeidsforhold fra Altinn

        if (personInfo == null) {
            return null;
        }

        LOG.info("Returnerer informasjon om arbeidstaker og arbeidsforhold for {}", personInfo.fødselsnummer());
        return new SlåOppArbeidstakerResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
                List.of(new ArbeidsforholdDto("Dummy arbeidsgiver", "00000000", "123456789")));
    }
}
