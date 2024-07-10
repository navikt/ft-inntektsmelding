package no.nav.familie.inntektsmelding.server.auth.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import no.nav.familie.inntektsmelding.server.auth.validator.TokenValidationHandler;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

public class JwtTokenValidationFilter implements Filter {

    private final TokenValidationHandler tokenValidationHandler;

    public JwtTokenValidationFilter(TokenValidationHandler tokenValidationHandler) {
        this.tokenValidationHandler = tokenValidationHandler;
    }

    @Override
    public void destroy() {
        // No specific implementation required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpServletRequest) {
            doTokenValidation(httpServletRequest, (HttpServletResponse) response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    private void doTokenValidation(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        KontekstHolder.setKontekst(tokenValidationHandler.getValidatedTokens(request));
        try {
            chain.doFilter(request, response);
        } finally {
            KontekstHolder.fjernKontekst();
        }
    }
}
