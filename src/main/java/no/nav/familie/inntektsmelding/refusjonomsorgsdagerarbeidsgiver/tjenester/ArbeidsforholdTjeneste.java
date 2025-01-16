package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.AaregRestKlient;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.ArbeidsforholdDto;

@ApplicationScoped
public class ArbeidsforholdTjeneste {
    private AaregRestKlient aaregRestKlient;
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdTjeneste.class);

    public ArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdTjeneste(AaregRestKlient aaregRestKlient) {
        this.aaregRestKlient = aaregRestKlient;
    }

    public List<ArbeidsforholdDto> hentNåværendeArbeidsforhold(PersonIdent ident) {
        var aaregInfo = aaregRestKlient.finnNåværendeArbeidsforholdForArbeidstaker(ident.getIdent());
        if (aaregInfo == null) {
            LOG.info("Fant ingen arbeidsforhold for ident {}. Returnerer tom liste", ident.getIdent());
            return Collections.emptyList();
        }
        LOG.info("Fant {} arbeidsforhold for ident {}.", aaregInfo.size(), ident.getIdent());
        return aaregInfo.stream().map(arbeidsforhold ->
            new ArbeidsforholdDto(
                arbeidsforhold.arbeidsgiver().identer().get(0).ident(), // TODO: hva trenger vi?
                arbeidsforhold.arbeidsgiver().identer().get(0).ident(),
                arbeidsforhold.navArbeidsforholdId().toString()
            )).toList();
    }
}
