package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.SlåOppArbeidstakerResponseDto;

@ApplicationScoped
public class ArbeidstakerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidstakerTjeneste.class);

    public SlåOppArbeidstakerResponseDto slåOppArbeidstaker(String fødselsnummer) {
        // TODO: Slå opp arbeidstaker
        // TODO: Sjekk at arbeidsgiver har tilgang til informasjon om den ansatte

        LOG.info("Returnerer dummy-data enn så lenge");
        return new SlåOppArbeidstakerResponseDto("Ola", null, "Nordmann",
            List.of(new ArbeidsforholdDto("Dummy arbeidsgiver", "00000000", "123456789")
        ));
    }
}
