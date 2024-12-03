package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;

import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.FantIkkeArbeidstakerException;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.InnsenderHarIkkeTilgangTilArbeidsforholdException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.dto.SlåOppArbeidstakerResponseDto;

import java.util.Collections;

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
        var personInfo = personTjeneste.hentPersonFraIdent(ident, Ytelsetype.OMSORGSPENGER);

        if (personInfo == null) {
            throw new FantIkkeArbeidstakerException();
        }

        var alleArbeidsforhold = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(ident);
        LOG.info("Fant totalt {} arbeidsforhold for ident {}", alleArbeidsforhold.size(), ident.getIdent());

        if (alleArbeidsforhold.isEmpty()) {
            LOG.info("Fant person, men ingen arbeidsforhold.");
            // Her kan man i fremtiden sjekke om arbeidstaker har en åpen sak i enten fp-sak eller k9-sak,
            // avhengig av ytelse som søkes etter
            // Enn så lenge returnerer vi en tom liste
            return new SlåOppArbeidstakerResponseDto(
                personInfo.fornavn(),
                personInfo.mellomnavn(),
                personInfo.etternavn(),
                Collections.emptyList()
            );
        }

        var arbeidsforholdInnsenderHarTilgangTil = alleArbeidsforhold
            .stream()
            .filter(dto -> altinnTilgangTjeneste.harTilgangTilBedriften(dto.underenhetId()))
            .toList();

        if (arbeidsforholdInnsenderHarTilgangTil.isEmpty()) {
            LOG.info("Innsender har ikke tilgang til noen arbeidsforhold for ident {}", ident.getIdent());
            throw new InnsenderHarIkkeTilgangTilArbeidsforholdException();
        }

        LOG.info("Innsender har tilgang til {} av {} arbeidsforhold for ident {}", arbeidsforholdInnsenderHarTilgangTil.size(), alleArbeidsforhold.size(), ident.getIdent());

        return new SlåOppArbeidstakerResponseDto(
            personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            arbeidsforholdInnsenderHarTilgangTil
        );
    }
}
