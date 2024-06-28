package no.nav.familie.inntektsmelding.server.auth;

import jakarta.ws.rs.NameBinding;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@NameBinding
public @interface AutentisertMed {
    OpenIDProvider issuer() default OpenIDProvider.AZUREAD;
}
