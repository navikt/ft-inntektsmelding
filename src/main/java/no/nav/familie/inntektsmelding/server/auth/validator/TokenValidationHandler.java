package no.nav.familie.inntektsmelding.server.auth.validator;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.server.auth.config.IssuerConfiguration;
import no.nav.familie.inntektsmelding.server.auth.config.MultiIssuerConfiguration;
import no.nav.familie.inntektsmelding.server.auth.jwt.JwtToken;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.kontekst.BasisKontekst;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.ConfigProvider;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;
import no.nav.vedtak.sikkerhet.oidc.validator.JwtUtil;
import no.nav.vedtak.sikkerhet.oidc.validator.OidcTokenValidator;
import no.nav.vedtak.sikkerhet.oidc.validator.OidcTokenValidatorConfig;

public class TokenValidationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TokenValidationHandler.class);
    private final MultiIssuerConfiguration config;

    public TokenValidationHandler(MultiIssuerConfiguration config) {
        this.config = config;
    }

    public Kontekst getValidatedTokens(HttpServletRequest request) {
        var unvalidatedToken = hentTokenFraHeader(request);
        Map<String, JwtToken> validatedTokens = unvalidatedToken.stream()
            .map(this::validate)
            .filter(entry -> entry != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, ConcurrentHashMap::new));

        return BasisKontekst.ikkeAutentisertRequest("sdfsd");
    }


    private Map.Entry<String, JwtToken> validate(JwtToken jwtToken) {
        try {
            LOG.debug("Check if token with issuer={} is present in config", jwtToken.getIssuer());
            var issuerConfig = getIssuerConfiguration(jwtToken);
            if (issuerConfig != null) {
                String issuerShortName = issuerConfig.getShortname();
                LOG.debug("Found token from trusted issuer={} with shortName={} in request", jwtToken.getIssuer(), issuerShortName);
                var validationResult = getValidator(jwtToken).validate(jwtToken.getTokenString());
                LOG.debug("Validated token from issuer[{}]", jwtToken.getIssuer());
                return new SimpleImmutableEntry<>(issuerShortName, jwtToken);
            } else {
                LOG.debug("Found token from unknown issuer[{}], skipping validation.", jwtToken.getIssuer());
                return null;
            }
        } catch (Exception e) {
            LOG.info("Found invalid token for issuer [{}, expires at {}], message:{} ",
                jwtToken.getIssuer(),
                jwtToken.getTokenClaims().getExpirationTime(),
                e.getMessage());
            return null;
        }
    }

    private static void validerTokenSetKontekst(TokenString tokenString, OpenIDProvider forventetTokenProvider) {
        try {
            Objects.requireNonNull(forventetTokenProvider, "Forventet token validator mangler");
            LOG.debug("Forventet token type: {}", forventetTokenProvider);

            var claims = JwtUtil.getClaims(tokenString.token());
            var tokenIssuer = Optional.ofNullable(JwtUtil.getIssuer(claims)).orElseThrow(() -> new WebApplicationException("Token mangler issuer."));
            LOG.debug("Issuer fra token: {}", tokenIssuer);

            var openIdKonfig = ConfigProvider.getOpenIDConfiguration(tokenIssuer)
                .orElseThrow(() -> new WebApplicationException("Issuer støttes ikke: " + tokenIssuer));

            var tokenProvider = openIdKonfig.type();
            if (!Objects.equals(tokenProvider, forventetTokenProvider)) {
                throw new WebApplicationException(String.format("Trenger en gyldig %s token, men har fått %s.",
                    forventetTokenProvider,
                    tokenProvider));
            }

            // Valider
            var tokenValidator = OidcTokenValidatorConfig.instance().getValidator(forventetTokenProvider);
            var validateResult = tokenValidator.validate(tokenString);

            // Håndter valideringsresultat
            if (validateResult.isValid()) {
                var expiresAt = Optional.ofNullable(JwtUtil.getExpirationTime(claims))
                    .orElseThrow(() -> new WebApplicationException("Token mangler expires at claim"));
                var token = new OpenIDToken(tokenProvider, OpenIDToken.OIDC_DEFAULT_TOKEN_TYPE, tokenString, null, expiresAt.toEpochMilli());
                KontekstHolder.setKontekst(RequestKontekst.forRequest(validateResult.subject(),
                    validateResult.compactSubject(),
                    validateResult.identType(),
                    token,
                    validateResult.getGrupper()));
                LOG.trace("token validert");
            } else {
                throw new WebApplicationException("Ugyldig token");
            }
        } catch (TekniskException e) {
            throw new WebApplicationException(e, Response.Status.FORBIDDEN);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
        }
    }

    private OidcTokenValidator getValidator(JwtToken jwtToken) {
        return getIssuerConfiguration(jwtToken).getTokenValidator();
    }

    private IssuerConfiguration getIssuerConfiguration(JwtToken jwtToken) {
        return getConfig().getIssuers().get(jwtToken.getIssuer());
    }

    public MultiIssuerConfiguration getConfig() {
        return config;
    }

    private Optional<JwtToken> hentTokenFraHeader(HttpServletRequest request) {
        var headerValue = Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION));

        return headerValue
            .filter(it -> it.startsWith(OpenIDToken.OIDC_DEFAULT_TOKEN_TYPE))
            .map(it -> new JwtToken(it.substring(OpenIDToken.OIDC_DEFAULT_TOKEN_TYPE.length())))
            .filter(jwtToken -> !config.getIssuers().containsKey(jwtToken.getIssuer()));
    }
}
