package no.nav.familie.inntektsmelding.forvaltning;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.rest.app.ProsessTaskApplikasjonTjeneste;
import no.nav.k9.prosesstask.rest.dto.ProsessTaskDataDto;
import no.nav.k9.prosesstask.rest.dto.ProsessTaskOpprettInputDto;
import no.nav.k9.prosesstask.rest.dto.ProsessTaskRestartInputDto;
import no.nav.k9.prosesstask.rest.dto.ProsessTaskRestartResultatDto;
import no.nav.k9.prosesstask.rest.dto.ProsessTaskRetryAllResultatDto;
import no.nav.k9.prosesstask.rest.dto.ProsessTaskSetFerdigInputDto;
import no.nav.k9.prosesstask.rest.dto.StatusFilterDto;

//TODO Når vi har funnet en metode for autentisering kan vi slette denne og heller bruke ProsessTaskRestTjeneste som ligger i k9-prosesstask
@AutentisertMedAzure
@OpenAPIDefinition(tags = @Tag(name = "prosesstask", description = "Håndtering av asynkrone oppgaver i form av prosesstask"))
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/prosesstask")
public class ProsessTaskRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ProsessTaskRestTjeneste.class);

    private ProsessTaskApplikasjonTjeneste prosessTaskApplikasjonTjeneste;
    private Tilgang tilgang;

    ProsessTaskRestTjeneste() {
        // REST CDI
    }

    @Inject
    public ProsessTaskRestTjeneste(ProsessTaskApplikasjonTjeneste prosessTaskApplikasjonTjeneste, Tilgang tilgang) {
        this.prosessTaskApplikasjonTjeneste = prosessTaskApplikasjonTjeneste;
        this.tilgang = tilgang;
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en prosess task i henhold til request", summary = "Oppretter en ny task klar for kjøring.", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "202", description = "Prosesstaskens oppdatert informasjon", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskDataDto.class))),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public ProsessTaskDataDto createProsessTask(
        @Parameter(description = "Informasjon for restart en eksisterende prosesstask") @Valid ProsessTaskOpprettInputDto inputDto) {
        sjekkAtKallerHarRollenDrift();
        // kjøres manuelt for å avhjelpe feilsituasjon, da er det veldig greit at det blir logget!
        LOG.info("Oppretter prossess task av type {}", inputDto.getTaskType());
        return prosessTaskApplikasjonTjeneste.opprettTask(inputDto);
    }

    @POST
    @Path("/launch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Restarter en eksisterende prosesstask.", summary =
        "En allerede FERDIG prosesstask kan ikke restartes. En prosesstask har normalt et gitt antall forsøk den kan kjøres automatisk. "
            +
            "Dette endepunktet vil tvinge tasken til å trigge uavhengig av maks antall forsøk", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Prosesstaskens oppdatert informasjon", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskRestartResultatDto.class))),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public ProsessTaskRestartResultatDto restartProsessTask(
        @Parameter(description = "Informasjon for restart en eksisterende prosesstask") @Valid @BeanParam
        ProsessTaskRestartInputDto restartInputDto) {
        sjekkAtKallerHarRollenDrift();
        // kjøres manuelt for å avhjelpe feilsituasjon, da er det veldig greit at det blir logget!
        LOG.info("Restarter prossess task {}", restartInputDto.getProsessTaskId());
        return prosessTaskApplikasjonTjeneste.flaggProsessTaskForRestart(restartInputDto);
    }

    @POST
    @Path("/retryall")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Restarter alle prosesstask med status FEILET.", summary = "Dette endepunktet vil tvinge feilede tasks til å trigge ett forsøk uavhengig av maks antall forsøk", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Response med liste av prosesstasks som restartes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskRetryAllResultatDto.class))),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public ProsessTaskRetryAllResultatDto retryAllProsessTask() {
        sjekkAtKallerHarRollenDrift();
        // kjøres manuelt for å avhjelpe feilsituasjon, da er det veldig greit at det blir logget!
        LOG.info("Restarter alle prossess task i status FEILET");
        return prosessTaskApplikasjonTjeneste.flaggAlleFeileteProsessTasksForRestart();
    }

    @POST
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lister prosesstasker med angitt status.", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Liste over prosesstasker, eller tom liste når angitt/default søkefilter ikke finner noen prosesstasker", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskDataDto.class)))
    })
    @Tilgangskontrollert
    public List<ProsessTaskDataDto> finnProsessTasks(@Parameter(description = "Liste av statuser som skal hentes.") @Valid StatusFilterDto statusFilterDto) {
        sjekkAtKallerHarRollenDrift();
        return prosessTaskApplikasjonTjeneste.finnAlle(statusFilterDto);
    }

    @POST
    @Path("/setferdig")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter feilet prosesstask med angitt prosesstask-id til FERDIG (kjøres ikke)", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Angitt prosesstask-id satt til status FERDIG"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response setFeiletProsessTaskFerdig(
        @Parameter(description = "Prosesstask-id for feilet prosesstask") @NotNull @Valid ProsessTaskSetFerdigInputDto prosessTaskIdDto) {
        sjekkAtKallerHarRollenDrift();
        prosessTaskApplikasjonTjeneste.setProsessTaskFerdig(prosessTaskIdDto.getProsessTaskId(), ProsessTaskStatus.valueOf(prosessTaskIdDto.getNaaVaaerendeStatus()));
        return Response.ok().build();
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
