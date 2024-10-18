package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(ForespørselRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForespørselRest {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRest.class);
    public static final String BASE_PATH = "/foresporsel";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private Tilgang tilgang;

    ForespørselRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public ForespørselRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste, Tilgang tilgang) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.tilgang = tilgang;
    }

    @POST
    @Path("/opprett")
    public Response opprettForespørsel(OpprettForespørselRequest request) {
        LOG.info("Mottok forespørsel om inntektsmeldingoppgave på fagsakSaksnummer {}", request.fagsakSaksnummer());
        sjekkErSystemkall();

        var bleForespørselOpprettet = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(request.skjæringstidspunkt(),
            KodeverkMapper.mapYtelsetype(request.ytelsetype()),
            new AktørIdEntitet(request.aktørId().id()),
            new OrganisasjonsnummerDto(request.orgnummer().orgnr()),
            request.fagsakSaksnummer());

        if (bleForespørselOpprettet.equals(ForespørselResultat.FORESPØRSEL_OPPRETTET)) {
            MetrikkerTjeneste.loggForespørselOpprettet(KodeverkMapper.mapYtelsetype(request.ytelsetype()));
        }
        return Response.ok(new OpprettForespørselResponse(bleForespørselOpprettet)).build();
    }

    @POST
    @Path("/oppdater")
    public Response oppdaterForespørsler(OppdaterForespørslerRequest request) {
        LOG.info("Mottok forespørsel om oppdatering av inntektsmeldingoppgaver på fagsakSaksnummer {}", request.fagsakSaksnummer());
        sjekkErSystemkall();

        forespørselBehandlingTjeneste.oppdaterForespørsler(
            KodeverkMapper.mapYtelsetype(request.ytelsetype()),
            new AktørIdEntitet(request.aktørId().id()),
            request.organisasjonerPerSkjæringstidspunkt(),
            request.fagsakSaksnummer()
        );

        return Response.ok().build();
    }

    @POST
    @Path("/lukk")
    public Response lukkForespørsel(LukkForespørselRequest request) {
        LOG.info("Lukk forespørsel for fagsakSaksnummer {} med orgnummer {} og skjæringstidspunkt {}",
            request.fagsakSaksnummer(),
            request.orgnummer(),
            request.skjæringstidspunkt());

        sjekkErSystemkall();

        forespørselBehandlingTjeneste.lukkForespørsel(request.fagsakSaksnummer(), request.orgnummer(), request.skjæringstidspunkt());
        return Response.ok().build();
    }

    @POST
    @Path("/sett-til-utgatt")
    public Response settForespørselTilUtgått(LukkForespørselRequest request) {
        LOG.info("Setter forespørsel for fagsakSaksnummer {} til utgått", request.fagsakSaksnummer());
        forespørselBehandlingTjeneste.settForespørselTilUtgått(request.fagsakSaksnummer(), request.orgnummer(), request.skjæringstidspunkt());
        return Response.ok().build();
    }

    record ForespørselDto(UUID uuid, OrganisasjonsnummerDto organisasjonsnummer, LocalDate skjæringstidspunkt, AktørIdDto brukerAktørId,
                          YtelseTypeDto ytelseType) {
    }

    static ForespørselDto mapTilDto(ForespørselEntitet entitet) {
        return new ForespørselDto(entitet.getUuid(), new OrganisasjonsnummerDto(entitet.getOrganisasjonsnummer()), entitet.getSkjæringstidspunkt(),
            new AktørIdDto(entitet.getAktørId().getAktørId()), KodeverkMapper.mapYtelsetype(entitet.getYtelseType()));
    }

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }
}

