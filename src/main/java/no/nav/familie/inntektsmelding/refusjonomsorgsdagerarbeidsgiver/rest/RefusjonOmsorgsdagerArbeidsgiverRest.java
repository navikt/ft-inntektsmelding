package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.RefusjonOmsorgsdagerArbeidsgiverRest.BASE_PATH;

@AutentisertMedTokenX
@ApplicationScoped
@Transactional
@Path(BASE_PATH)
public class RefusjonOmsorgsdagerArbeidsgiverRest {
    private static final Logger LOG = LoggerFactory.getLogger(RefusjonOmsorgsdagerArbeidsgiverRest.class);

    public static final String BASE_PATH = "/imdialog/refusjon-omsorgsdager-arbeidsgiver";
    private static final String HENT_OPPLYSNINGER = "/opplysninger";
    private static final String SLÅ_OPP_ARBEIDSTAKER = "/arbeidstaker";
    private static final String SEND_SOKNAD = "/send-soknad";
    private static final String LAST_NED_PDF = "/last-ned-pdf";

    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    RefusjonOmsorgsdagerArbeidsgiverRest() {
        // CDI
    }

    @Inject
    public RefusjonOmsorgsdagerArbeidsgiverRest(ArbeidstakerTjeneste arbeidstakerTjeneste) {
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
    }

    @POST
    @Path(SLÅ_OPP_ARBEIDSTAKER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter opplysninger om arbeidstaker, gitt et fødselsnummer.", tags = "imdialog")
    @Tilgangskontrollert
    public Response slåOppArbeidstaker(
        @Parameter(description = "Datapakke som inneholder fødselsnummeret til en arbeidstaker") @NotNull
        @Valid SlåOppArbeidstakerDto slåOppArbeidstakerDto) {

        LOG.info("Slår opp arbeidstaker med fødselsnummer {}", slåOppArbeidstakerDto.fødselsnummer());
        var dto = arbeidstakerTjeneste.slåOppArbeidstaker(slåOppArbeidstakerDto.fødselsnummer());
        return Response.ok(dto).build();

    }
}
