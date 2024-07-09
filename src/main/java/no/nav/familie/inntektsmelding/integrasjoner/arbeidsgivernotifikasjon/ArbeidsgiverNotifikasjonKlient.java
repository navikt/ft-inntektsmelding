package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonErrorHandler.handleError;
import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonErrorHandler.handleValidationError;

import java.net.http.HttpRequest;

import jakarta.enterprise.context.Dependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "arbeidsgiver.notifikasjon.url", endpointDefault = "http://notifikasjon-produsent-api.fager/api/graphql", scopesProperty = "arbeidsgiver.notifikasjon.scopes", scopesDefault = "api://prod-gcp.fager.notifikasjon-produsent-api/.default")
class ArbeidsgiverNotifikasjonKlient {

    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverNotifikasjonKlient.class);

    private static final String ERROR_RESPONSE = "F-102030";

    private final RestClient restKlient;
    private final RestConfig restConfig;

    ArbeidsgiverNotifikasjonKlient() {
        this(RestClient.client());
    }

    public ArbeidsgiverNotifikasjonKlient(RestClient restKlient) {
        this.restKlient = restKlient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public String opprettSak(NySakMutationRequest request, NySakResultatResponseProjection projection) {
        LOG.info("FAGER: Opprett Sak");
        var resultat = query(new GraphQLRequest(request, projection), NySakMutationResponse.class).nySak();
        if (resultat instanceof NySakVellykket vellykket) {
            LOG.info("Vellykket oppretelse av sak");
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "opprettelse av ny sak");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public HentetSak hentSakMedGrupperingsid(HentSakMedGrupperingsidQueryRequest request, HentSakResultatResponseProjection projection) {
        LOG.info("FAGER: Hen Sak med grupperingsid");
        var resultat = query(new GraphQLRequest(request, projection), HentSakMedGrupperingsidQueryResponse.class).hentSakMedGrupperingsid();
        if (resultat instanceof HentetSak sak) {
            return sak;
        } else {
            loggFeilmelding((Error) resultat, "hent sak med grupperingsid");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public HentetSak hentSak(HentSakQueryRequest request, HentSakResultatResponseProjection projection) {
        LOG.info("FAGER: Hen Sak");
        var resultat = query(new GraphQLRequest(request, projection), HentSakQueryResponse.class).hentSak();
        if (resultat instanceof HentetSak sak) {
            return sak;
        } else {
            loggFeilmelding((Error) resultat, "hent sak med grupperingsid");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public String oppdaterSakStatusMedGrupperingsid(NyStatusSakByGrupperingsidMutationRequest request,
                                                    NyStatusSakResultatResponseProjection projection) {
        LOG.info("FAGER: Oppdater sak status med grupperingsid");
        var resultat = query(new GraphQLRequest(request, projection), NyStatusSakByGrupperingsidMutationResponse.class).nyStatusSakByGrupperingsid();
        if (resultat instanceof NyStatusSakVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "oppdater sak status");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public String opprettOppgave(NyOppgaveMutationRequest request, NyOppgaveResultatResponseProjection projection) {
        LOG.info("FAGER: Opprett Oppgave");
        var resultat = query(new GraphQLRequest(request, projection), NyOppgaveMutationResponse.class).nyOppgave();
        if (resultat instanceof NyOppgaveVellykket vellykket) {
            LOG.info("Vellykket oppretelse av oppgave");
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "opprettelse av ny oppgave");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public String lukkOppgave(OppgaveUtfoertMutationRequest request, OppgaveUtfoertResultatResponseProjection projection) {
        LOG.info("FAGER: Lukk Oppgave");
        var resultat = query(new GraphQLRequest(request, projection), OppgaveUtfoertMutationResponse.class).oppgaveUtfoert();
        if (resultat instanceof OppgaveUtfoertVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "lukking av oppgave");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public String lukkOppgaveByEksternId(OppgaveUtfoertByEksternId_V2MutationRequest request, OppgaveUtfoertResultatResponseProjection projection) {
        LOG.info("FAGER: Lukk Oppgave by ekstern id");
        var resultat = query(new GraphQLRequest(request, projection),
            OppgaveUtfoertByEksternId_V2MutationResponse.class).oppgaveUtfoertByEksternId_V2();
        if (resultat instanceof OppgaveUtfoertVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "lukking av oppgave by ekstern id");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public String oppdaterSakStatus(NyStatusSakMutationRequest request, NyStatusSakResultatResponseProjection projection) {
        LOG.info("FAGER: Lukk Oppgave");
        var resultat = query(new GraphQLRequest(request, projection), NyStatusSakMutationResponse.class).nyStatusSak();
        if (resultat instanceof NyStatusSakVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "ny status sak");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }


    private <T extends GraphQLResult<?>> T query(GraphQLRequest req, Class<T> clazz) {
        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(req.toHttpJsonBody()));
        var restRequest = RestRequest.newRequest(method, restConfig.endpoint(), restConfig);
        var response = restKlient.sendReturnUnhandled(restRequest);
        LOG.info("FAGER: Svar med code: {} og body: {}", response.statusCode(), response.body());
        var res = handleResponse(response.body(), clazz);
        if (res != null && res.hasErrors()) {
            return handleError(res.getErrors(), restConfig.endpoint(), ERROR_RESPONSE);
        }
        return res;
    }

    private <T> T handleResponse(String response, Class<T> clazz) {
        if (response == null) {
            return null;
        }
        if (clazz.isAssignableFrom(String.class)) {
            return clazz.cast(response);
        }
        LOG.info("FAGER: Response: {} til class {}", response, clazz);
        return DefaultJsonMapper.fromJson(response, clazz);
    }

    private static void loggFeilmelding(Error feil, String action) {
        handleValidationError("Funksjonellfeil", feil.getFeilmelding(), action);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [endpoint=" + restConfig.endpoint() + "]";
    }
}
