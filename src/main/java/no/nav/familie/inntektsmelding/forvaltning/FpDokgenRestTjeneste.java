package no.nav.familie.inntektsmelding.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.NaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonPeriodeEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.Autentisert;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@ApplicationScoped
@Path(FpDokgenRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Autentisert
public class FpDokgenRestTjeneste {
    public static final String BASE_PATH = "/inntektsmelding-pdf";
    private static final Logger LOG = LoggerFactory.getLogger(FpDokgenTjeneste.class);
    private static final boolean IS_PROD = Environment.current().isProd();
    private FpDokgenTjeneste fpDokgenTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;


    public FpDokgenRestTjeneste() {
        //CDI
    }

    @Inject
    public FpDokgenRestTjeneste(FpDokgenTjeneste fpDokgenTjeneste, InntektsmeldingRepository inntektsmeldingRepository) {
        this.fpDokgenTjeneste = fpDokgenTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces("application/pdf")
    @UtenAutentisering
    @Operation(description = "Generer en pdf av en inntektsmelding", tags = "forvaltning")
    public Response genererPdf(@Valid @NotNull InntektsmeldingRequest inntektsmeldingRequest) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        InntektsmeldingEntitet inntektsmeldingEntitet;
        if (inntektsmeldingRequest.inntektsmeldingId != null) {
            //Genererer en pdf på eksisterende inntektsmelding
            inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingRequest.inntektsmeldingId.intValue());
            LOG.info("Generer en pdf av en inntektsmelding med id: {}", inntektsmeldingRequest.inntektsmeldingId);
        } else {
            //TODO Vurdere å fjerne før release
            var builder = InntektsmeldingEntitet.builder()
                .medAktørId(AktørIdEntitet.dummy())
                .medKontaktperson(new KontaktpersonEntitet(inntektsmeldingRequest.kontaktpersonNavn, inntektsmeldingRequest.kontaktpersonTlf))
                .medMånedInntekt(inntektsmeldingRequest.maanedInntekt())
                .medYtelsetype(mapYtelseType(inntektsmeldingRequest.ytelsetype()))
                .medOpprettetTidspunkt(LocalDateTime.now())
                .medStartDato(inntektsmeldingRequest.startdatoPermisjon())
                .medArbeidsgiverIdent(inntektsmeldingRequest.arbeidsgiverIdent());

            if (inntektsmeldingRequest.refusjonsperioder() != null) {
                builder.medRefusjonsPeriode(mapRefusjonsperiode(inntektsmeldingRequest.refusjonsperioder()));
            }
            if (inntektsmeldingRequest.naturalytelser() != null) {
                builder.medNaturalYtelse(mapNaturalytelser(inntektsmeldingRequest.naturalytelser));
            }
            inntektsmeldingEntitet = builder.build();
        }

        var pdf = fpDokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet);

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=dokument.pdf");
        return responseBuilder.build();
    }

    private List<RefusjonPeriodeEntitet> mapRefusjonsperiode(List<RefusjonPeriodeDto> refusjonsperioder) {
        return refusjonsperioder.stream().map(periode -> new RefusjonPeriodeEntitet(periode.fom(), periode.tom(), periode.beloep())).toList();
    }

    private List<NaturalytelseEntitet> mapNaturalytelser(List<NaturalYtelseDto> naturalYtelser) {
        return naturalYtelser.stream()
            .map(periode -> new NaturalytelseEntitet.Builder().medPeriode(periode.fom(), periode.tom())
                .medType(periode.type())
                .medBeløp(periode.beloep())
                .medErBortfalt(periode.erBortfalt())
                .build())
            .toList();
    }

    private Ytelsetype mapYtelseType(String ytelsetype) {
        return switch (ytelsetype.toLowerCase()) {
            case "foreldrepenger" -> Ytelsetype.FORELDREPENGER;
            case "svangerskapspenger" -> Ytelsetype.SVANGERSKAPSPENGER;
            case "pleiepenger sykt barn" -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case "pleiepenger nærstående" -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
            case "opplæringspenger" -> Ytelsetype.OPPLÆRINGSPENGER;
            case "omsorgspenger" -> Ytelsetype.OMSORGSPENGER;
            default -> throw new IllegalArgumentException("Ugyldig ytelsetype: " + ytelsetype);
        };
    }

    public record InntektsmeldingRequest(Long inntektsmeldingId, String ytelsetype, String arbeidsgiverIdent, String kontaktpersonNavn,
                                         String kontaktpersonTlf, LocalDate startdatoPermisjon,
                                         @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal maanedInntekt,
                                         List<RefusjonPeriodeDto> refusjonsperioder, List<NaturalYtelseDto> naturalytelser) {
    }

    public record RefusjonPeriodeDto(@NotNull LocalDate fom, @NotNull LocalDate tom,
                                     @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloep) {
    }

    public record NaturalYtelseDto(@NotNull LocalDate fom, @NotNull LocalDate tom, NaturalytelseType type,
                                   @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloep,
                                   Boolean erBortfalt) {
    }
}
