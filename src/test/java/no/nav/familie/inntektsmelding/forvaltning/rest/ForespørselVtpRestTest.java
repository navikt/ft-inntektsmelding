package no.nav.familie.inntektsmelding.forvaltning.rest;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.konfig.Environment;

@ExtendWith(MockitoExtension.class)
public class ForespørselVtpRestTest {

    private static final ForespørselVtpRest forespørselVtpRest = new ForespørselVtpRest(null);

    @Mock
    Environment environment;

    @Test
    public void skal_kaste_exception_om_i_prod() {
        when(environment.isLocal()).thenReturn(Boolean.FALSE);
        when(environment.isVTP()).thenReturn(Boolean.FALSE);
        forespørselVtpRest.setEnvironment(environment);
        assertThrows(RuntimeException.class, () -> forespørselVtpRest.finnForespoerselForSaksnummer(null));
    }
}
