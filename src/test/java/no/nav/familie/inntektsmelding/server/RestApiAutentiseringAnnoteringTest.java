package no.nav.familie.inntektsmelding.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.server.auth.api.Autentisert;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMed;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

/**
 * Sjekker at alle REST endepunkt har definert autorisasjon konfigurasjon for Autentisering.
 * Alle REST tjenester må ha en @Autorisert annotering enten på klasse nivå eller på metode nivå.
 */
class RestApiAutentiseringAnnoteringTest {

    protected static final List<Class<? extends Annotation>> GYLDIGE_ANNOTERINGER = List.of(Autentisert.class,
        AutentisertMed.class,
        UtenAutentisering.class);

    @Test
    void test_at_alle_restmetoder_er_annotert_med_gyldig_annotering() {
        for (var restMethod : RestApiTester.finnAlleRestMetoder()) {
            assertThat(finnGyldigAnnotering(restMethod))
                .withFailMessage(String.format("Mangler @%s eller @%s -annotering på %s",
                    Autentisert.class.getSimpleName(),
                    AutentisertMed.class.getSimpleName(),
                    restMethod))
                .isNotNull();
        }
    }

    private Annotation finnGyldigAnnotering(Method method) {
        return findAnnotation(method.getAnnotations())
            .or(() -> findAnnotation(method.getDeclaringClass().getAnnotations()))
            .orElse(null);
    }

    private Optional<Annotation> findAnnotation(Annotation[] annotations) {
        return Arrays.stream(annotations)
            .filter(a -> RestApiAutentiseringAnnoteringTest.GYLDIGE_ANNOTERINGER.contains(a.annotationType()))
            .findFirst();
    }

}
