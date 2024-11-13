package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.SlåOppArbeidstakerResponseDto;

@ApplicationScoped
public class ArbeidstakerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidstakerTjeneste.class);
    private PersonTjeneste personTjeneste;
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    public ArbeidstakerTjeneste() {
        // CDI
    }

    public ArbeidstakerTjeneste(PersonTjeneste personTjeneste, ArbeidsforholdTjeneste arbeidsforholdTjeneste, AltinnTilgangTjeneste altinnTilgangTjeneste) {
        this.personTjeneste = personTjeneste;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.altinnTilgangTjeneste = altinnTilgangTjeneste;
    }

    public SlåOppArbeidstakerResponseDto slåOppArbeidstaker(PersonIdent ident) {
        var personInfo = personTjeneste.hentPersonFraIdent(ident, Ytelsetype.OMSORGSDAGER);

        // TODO: Sjekk tilganger til å hente arbeidsforhold fra Altinn

        if (personInfo == null) {
            return null;
        }

        var arbeidsforhold = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(ident)
            .stream()
            .filter(dto -> altinnTilgangTjeneste.harTilgangTilBedriften(dto.underenhetId()))
            .toList();


        LOG.info("Returnerer informasjon om arbeidstaker og arbeidsforhold for {}", personInfo.fødselsnummer());
        return new SlåOppArbeidstakerResponseDto(
            personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            arbeidsforhold
        );
    }
}
