package no.nav.familie.inntektsmelding.forvaltning;

import java.net.HttpURLConnection;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.rest.app.ProsessTaskApplikasjonTjeneste;
import no.nav.vedtak.felles.prosesstask.rest.dto.FeiletProsessTaskDataDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskDataDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskOpprettInputDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskRestartInputDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskRestartResultatDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskRetryAllResultatDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskStatusDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.StatusFilterDto;

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
    @Path("/launch/{taskId}/{taskStatus}")
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
        @Parameter(description = "Informasjon for restart en eksisterende prosesstask") @Valid @NotNull
        @PathParam("taskStatus") FeiletTaskStatus feiletTaskStatus, @NotNull @PathParam("taskId") Long prosessTaskId) {
        sjekkAtKallerHarRollenDrift();
        // kjøres manuelt for å avhjelpe feilsituasjon, da er det veldig greit at det blir logget!
        LOG.info("Restarter prossess task {}", prosessTaskId);

        var restartInputDto = new ProsessTaskRestartInputDto();
        restartInputDto.setNaaVaaerendeStatus(feiletTaskStatus.name());
        restartInputDto.setProsessTaskId(prosessTaskId);
        return prosessTaskApplikasjonTjeneste.flaggProsessTaskForRestart(restartInputDto);
    }

    public enum FeiletTaskStatus {
        FEILET,
        VENTER_SVAR,
        SUSPENDERT;
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
    @Path("/list/{taskStatus}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lister prosesstasker med angitt status.", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Liste over prosesstasker, eller tom liste når angitt/default søkefilter ikke finner noen prosesstasker", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProsessTaskDataDto.class)))
    })
    @Tilgangskontrollert
    public List<ProsessTaskDataDto> finnProsessTasks(
        @Parameter(description = "Liste av statuser som skal hentes.") @Valid @PathParam("taskStatus")
        ProsessTaskRestTjeneste.AlleIkkeFerdigStatus finnTaskStatus) {
        sjekkAtKallerHarRollenDrift();
        var statusFilterDto = new StatusFilterDto();
        statusFilterDto.setProsessTaskStatuser(List.of(new ProsessTaskStatusDto(finnTaskStatus.name())));
        return prosessTaskApplikasjonTjeneste.finnAlle(statusFilterDto);
    }

    public enum AlleIkkeFerdigStatus {
        FEILET,
        VENTER_SVAR,
        SUSPENDERT,
        VETO,
        KLAR;
    }

    @POST
    @Path("/feil/{taskId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter informasjon om feilet prosesstask med angitt prosesstask-id", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Angitt prosesstask-id finnes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeiletProsessTaskDataDto.class))),
        @ApiResponse(responseCode = "404", description = "Tom respons når angitt prosesstask-id ikke finnes"),
        @ApiResponse(responseCode = "400", description = "Feil input")
    })
    @Tilgangskontrollert
    public Response finnFeiletProsessTask(
        @NotNull @Parameter(description = "Prosesstask-id for feilet prosesstask") @Valid @PathParam("taskId") Long prosessTaskId) {
        sjekkAtKallerHarRollenDrift();
        var resultat = prosessTaskApplikasjonTjeneste.finnFeiletProsessTask(prosessTaskId);
        if (resultat.isPresent()) {
            return Response.ok(resultat.get()).build();
        }
        return Response.status(HttpURLConnection.HTTP_NOT_FOUND).build();
    }

    @POST
    @Path("/setferdig/{taskId}/{taskStatus}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter feilet prosesstask med angitt prosesstask-id til FERDIG (kjøres ikke)", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "Angitt prosesstask-id satt til status FERDIG"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response setFeiletProsessTaskFerdig(
        @NotNull @Parameter(description = "Prosesstask-id for feilet prosesstask") @Valid
        @PathParam("taskStatus") ProsessTaskRestTjeneste.AlleIkkeFerdigStatus naavarendeTaskStatus,
        @NotNull @PathParam("taskId") Long prosessTaskId) {
        sjekkAtKallerHarRollenDrift();
        prosessTaskApplikasjonTjeneste.setProsessTaskFerdig(prosessTaskId,
            ProsessTaskStatus.valueOf(naavarendeTaskStatus.name()));
        return Response.ok().build();
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
