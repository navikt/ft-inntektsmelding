package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingDialogTjeneste;
import no.nav.familie.inntektsmelding.server.auth.AutentisertMed;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;

@Path(InntektsmeldingDialogRest.BASE_PATH)
@ApplicationScoped
@Transactional
@AutentisertMed(issuer = OpenIDProvider.TOKENX)
public class InntektsmeldingDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingDialogRest.class);

    public static final String BASE_PATH = "/imdialog";
    private static final String HENT_GRUNNLAG = "/grunnlag";
    private static final String SEND_INNTEKTSMELDING = "/send-inntektsmelding";
    private InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste;

    @Inject
    public InntektsmeldingDialogRest(InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste) {
        this.inntektsmeldingDialogTjeneste = inntektsmeldingDialogTjeneste;
    }

    InntektsmeldingDialogRest() {
        // CDI
    }

    @GET
    @Path(HENT_GRUNNLAG)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter et grunnlag av all data vi har om søker, inntekt og arbeidsforholdet.", tags = "imdialog")
    public Response hentInnsendingsinfo(
        @Parameter(description = "Henter et grunnlag av all data vi har om søker, inntekt og arbeidsforholdet basert på en forespørsel UUID") @NotNull
        @QueryParam("foresporselUuid") UUID forespørselUuid) {
        LOG.info("Henter grunnlag for forespørsel " + forespørselUuid);
        var dto = inntektsmeldingDialogTjeneste.lagDialogDto(forespørselUuid);
        return Response.ok(dto).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sender inn inntektsmelding", tags = "imdialog")
    public Response sendInntektsmelding(@Parameter(description = "Datapakke med informasjon om inntektsmeldingen") @NotNull @Valid
                                        SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        LOG.info("Mottok inntektsmelding for forespørsel " + sendInntektsmeldingRequestDto.foresporselUuid());
        inntektsmeldingDialogTjeneste.mottaInntektsmelding(sendInntektsmeldingRequestDto);
        return Response.ok(sendInntektsmeldingRequestDto).build();
    }
}
