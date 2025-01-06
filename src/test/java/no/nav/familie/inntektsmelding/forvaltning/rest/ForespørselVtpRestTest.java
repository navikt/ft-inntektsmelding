package no.nav.familie.inntektsmelding.forvaltning.rest;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;


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
        System.setProperty("NAIS_CLUSTER_NAME", "prod-fss");
        assertThrows(RuntimeException.class, () -> forespørselVtpRest.finnForespoerselForSaksnummer(null));
        System.clearProperty("NAIS_CLUSTER_NAME");
    }

    @Test
    void skal_ikke_kaste_exception_om_i_vtp() {
        try (var environment = Mockito.mockStatic(Environment.class, Answers.CALLS_REAL_METHODS)) {
            var vtpEnv = mock(Environment.class);
            when(vtpEnv.isLocal()).thenReturn(true);
            environment.when(Environment::current).thenReturn(vtpEnv);
            var response = forespørselVtpRest.finnForespoerselForSaksnummer(null);
            assertThat(response).isNotNull();
        }
    }

}
