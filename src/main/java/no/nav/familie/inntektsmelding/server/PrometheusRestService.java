package no.nav.familie.inntektsmelding.server;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

import io.swagger.v3.oas.annotations.Operation;

@Path("/metrics")
@Produces(TEXT_PLAIN)
@ApplicationScoped
public class PrometheusRestService {

    @GET
    @Operation(hidden = true)
    @Path("/prometheus")
    @UtenAutentisering
    public String prometheus() {
        return REGISTRY.scrape();
    }
}
