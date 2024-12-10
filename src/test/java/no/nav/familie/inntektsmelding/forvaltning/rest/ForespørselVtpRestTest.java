package no.nav.familie.inntektsmelding.forvaltning.rest;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.konfig.Environment;

@ExtendWith(MockitoExtension.class)
public class ForespørselVtpRestTest {

    private static final ForespørselVtpRest forespørselVtpRest = new ForespørselVtpRest(mock(ForespørselBehandlingTjeneste.class, Answers.CALLS_REAL_METHODS));

    @Mock
    Environment environment;


    @Test
    public void skal_kaste_exception_om_i_prod() {
        when(environment.isLocal()).thenReturn(Boolean.FALSE);
        when(environment.isVTP()).thenReturn(Boolean.FALSE);
        forespørselVtpRest.setEnvironment(environment);
        assertThrows(RuntimeException.class, () -> forespørselVtpRest.finnForespoerselForSaksnummer(null));
    }

    @Test
    public void skal_ikke_kaste_exception_om_i_vtp() {
        when(environment.isLocal()).thenReturn(Boolean.FALSE);
        when(environment.isVTP()).thenReturn(Boolean.TRUE);
        forespørselVtpRest.setEnvironment(environment);
        forespørselVtpRest.finnForespoerselForSaksnummer(null);
    }
}
