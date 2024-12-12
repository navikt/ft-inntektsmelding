package no.nav.familie.inntektsmelding.forvaltning.rest;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.konfig.Environment;

@ExtendWith(MockitoExtension.class)
class ForespørselVtpRestTest {

    private static final ForespørselVtpRest forespørselVtpRest = new ForespørselVtpRest(mock(ForespørselBehandlingTjeneste.class,
        Answers.CALLS_REAL_METHODS));


    @Test
    void skal_kaste_exception_om_i_prod() {
        Mockito.mockStatic(Environment.class, Answers.RETURNS_DEEP_STUBS);
        assertThrows(RuntimeException.class, () -> forespørselVtpRest.finnForespoerselForSaksnummer(null));
    }

    @Test
    void skal_ikke_kaste_exception_om_i_vtp() {
        try (var environment = Mockito.mockStatic(Environment.class, Answers.CALLS_REAL_METHODS)) {
            var vtpEnv = mock(Environment.class);
            when(vtpEnv.isLocal()).thenReturn(true);
            environment.when(Environment::current).thenReturn(vtpEnv);
            forespørselVtpRest.finnForespoerselForSaksnummer(null);
        }
    }

}
