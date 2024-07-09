package no.nav.familie.inntektsmelding.server;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.familie.inntektsmelding.forespørsel.rest.ForespørselRest;
import no.nav.familie.inntektsmelding.forvaltning.FagerTestRestTjeneste;
import no.nav.familie.inntektsmelding.forvaltning.FpDokgenRestTjeneste;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogRest;
import no.nav.familie.inntektsmelding.server.auth.AuthenticationFilter;
import no.nav.familie.inntektsmelding.server.exceptions.ConstraintViolationMapper;
import no.nav.familie.inntektsmelding.server.exceptions.GeneralRestExceptionMapper;
import no.nav.familie.inntektsmelding.server.exceptions.JsonMappingExceptionMapper;
import no.nav.familie.inntektsmelding.server.exceptions.JsonParseExceptionMapper;
import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ApiConfig.class);
    public static final String API_URI = "/api";
    private static final Environment ENV = Environment.current();

    public ApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        // Sikkerhet
        registerAuthenticationFilter();
        registerOpenApi();

        registerRestServices();
        registerExceptionMappers();
        register(JacksonJsonConfig.class);

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    void registerRestServices() {
        registerClasses(getApplicationClasses());
    }

    void registerAuthenticationFilter() {
        register(AuthenticationFilter.class);
    }

    void registerExceptionMappers() {
        register(GeneralRestExceptionMapper.class);
        register(ConstraintViolationMapper.class);
        register(JsonMappingExceptionMapper.class);
        register(JsonParseExceptionMapper.class);
    }

    private void registerOpenApi() {
        var oas = new OpenAPI();
        var info = new Info().title(ENV.getNaisAppName())
            .version(Optional.ofNullable(ENV.imageName()).orElse("1.0"))
            .description("REST grensesnitt for FPINNTEKTSMELDING.");

        oas.info(info).addServersItem(new Server().url(ENV.getProperty("context.path", "/fpinntektsmelding")));
        var oasConfig = new SwaggerConfiguration().openAPI(oas)
            .prettyPrint(true)
            .resourceClasses(getApplicationClasses().stream().map(Class::getName).collect(Collectors.toSet()));
        try {
            new GenericOpenApiContextBuilder<>().openApiConfiguration(oasConfig).buildContext(true).read();
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }

        register(OpenApiResource.class);
    }

    private Set<Class<?>> getApplicationClasses() {
        return Set.of(ForespørselRest.class, InntektsmeldingDialogRest.class, FagerTestRestTjeneste.class, ProsessTaskRestTjeneste.class,
            FpDokgenRestTjeneste.class);
    }

    private Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}
