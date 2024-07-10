package no.nav.familie.inntektsmelding.server.auth.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@NameBinding
public @interface AutentisertMed {
    OpenIDProvider issuer() default OpenIDProvider.AZUREAD;
}
