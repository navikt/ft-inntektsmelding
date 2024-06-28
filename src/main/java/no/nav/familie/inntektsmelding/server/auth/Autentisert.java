package no.nav.familie.inntektsmelding.server.auth;


import jakarta.ws.rs.NameBinding;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@NameBinding
public @interface Autentisert {
}
