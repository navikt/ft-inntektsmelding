package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.AaregRestKlient;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.dto.ArbeidsforholdDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class ArbeidsforholdTjeneste {
    private AaregRestKlient aaregRestKlient;
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdTjeneste.class);

    public ArbeidsforholdTjeneste() {
        // CDI
    }

    public ArbeidsforholdTjeneste(AaregRestKlient aaregRestKlient) {
        this.aaregRestKlient = aaregRestKlient;
    }

    public List<ArbeidsforholdDto> hentNåværendeArbeidsforhold(PersonIdent ident) {
        var aaregInfo = aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.getIdent(), LocalDate.now(), LocalDate.now());
        if (aaregInfo == null) {
            LOG.info("Fant ingen arbeidsforhold for ident {}. Returnerer tom liste", ident.getIdent());
            return Collections.emptyList();
        }
        LOG.info("Fant {} arbeidsforhold for ident {}.", aaregInfo.size(), ident.getIdent());
        return aaregInfo.stream().map(arbeidsforhold ->
            new ArbeidsforholdDto(
                arbeidsforhold.arbeidsgiver().offentligIdent(),
                arbeidsforhold.arbeidsgiver().organisasjonsnummer(),
                arbeidsforhold.arbeidsforholdId()
            )).toList();
    }
}
