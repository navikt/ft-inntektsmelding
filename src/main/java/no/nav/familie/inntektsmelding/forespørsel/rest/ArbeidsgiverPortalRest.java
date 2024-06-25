package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
@Transactional
@Path(ArbeidsgiverPortalRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ArbeidsgiverPortalRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverPortalRest.class);
    public static final String BASE_PATH = "/portal";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private ForespørselTjeneste forespørselTjeneste;

    public ArbeidsgiverPortalRest() {
    }

    @Inject
    public ArbeidsgiverPortalRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste, ForespørselTjeneste forespørselTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/foresporsel/opprett")
    @Operation(description = "Oppretter en forespørsel om inntektsmelding", tags = "forespørsel")
    public Response opprettForespørsel(OpprettForespørselRequest request) {
        LOG.info("Mottok forespørsel om inntektsmeldingoppgave på saksnummer " + request.saksnummer());
        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(request.skjæringstidspunkt(), KodeverkMapper.mapYtelsetype(request.ytelsetype()),
            new AktørIdEntitet(request.aktørId().id()), new OrganisasjonsnummerDto(request.orgnummer().orgnr()), request.saksnummer());
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/sak/ferdigstill")
    @Operation(description = "Ferdigstiller en sak på min side arbeidsgiver", tags = "sak")
    public Response ferdigstillSak(SakRequest request) {
        forespørselBehandlingTjeneste.ferdigstillSak(new AktørIdEntitet(request.aktørId().id()),
            new OrganisasjonsnummerDto(request.orgnummer().orgnr()), KodeverkMapper.mapYtelsetype(request.ytelsetype()), request.saksnummer());
        return Response.ok().build();
    }


    /**
     * @deprecated See på InntektsmeldingDialogRest.hentInnsendingsinfo()
     * @param forespørselUUID
     */
    @Deprecated(forRemoval = true, since = "18.06.2024")
    @GET
    @Path("/foresporsel/{uuid}")
    @Operation(description = "Henter en forespørsel for gitt UUID", tags = "forespørsel")
    public Response readForespørsel(@PathParam("uuid") UUID forespørselUUID) {
        return Response.ok(forespørselTjeneste.finnForespørsel(forespørselUUID).map(ArbeidsgiverPortalRest::mapTilDto).orElseThrow()).build();
    }

    record ForespørselDto(UUID uuid, OrganisasjonsnummerDto organisasjonsnummer, LocalDate skjæringstidspunkt, AktørIdDto brukerAktørId, YtelseTypeDto ytelseType) {}

    static ForespørselDto mapTilDto(ForespørselEntitet entitet) {
        var sak = entitet.getSak();
        return new ForespørselDto(
            entitet.getUuid(),
            new OrganisasjonsnummerDto(sak.getOrganisasjonsnummer()),
            entitet.getSkjæringstidspunkt(),
            new AktørIdDto(sak.getAktørId().getAktørId()),
            KodeverkMapper.mapYtelsetype(sak.getYtelseType()));
    }
}

