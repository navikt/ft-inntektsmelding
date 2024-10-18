package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(InntektsmeldingFpsakRest.BASE_PATH)
public class InntektsmeldingFpsakRest {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingFpsakRest.class);

    public static final String BASE_PATH = "/overstyring";
    private static final String INNTEKTSMELDING = "/inntektsmelding";
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private Tilgang tilgangskontroll;

    InntektsmeldingFpsakRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingFpsakRest(InntektsmeldingTjeneste inntektsmeldingTjeneste, Tilgang tilgangskontroll) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.tilgangskontroll = tilgangskontroll;
    }

    @POST
    @Path(INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sender inn inntektsmelding fra fpsak", tags = "imdialog")
    public Response sendInntektsmelding(@Parameter(description = "Datapakke med informasjon om inntektsmeldingen") @NotNull @Valid
                                        SendOverstyrtInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        LOG.info("Mottok overstyrt inntektsmelding fra saksbehandler {}", sendInntektsmeldingRequestDto.opprettetAv());
        tilgangskontroll.sjekkErSystembruker();

        inntektsmeldingTjeneste.mottaOverstyrtInntektsmelding(sendInntektsmeldingRequestDto);
        return Response.ok().build();
    }
}
