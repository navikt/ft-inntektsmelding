package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.TelefonnummerResponseProjection;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.person.Persondata;

@ApplicationScoped
public class PersonTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(PersonTjeneste.class);
    private PdlKlient pdlKlient;

    PersonTjeneste() {
        // CDI
    }

    @Inject
    public PersonTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public PersonInfo hentPersonInfoFraAktørId(AktørIdEntitet aktørId, Ytelsetype ytelseType) {
        var request = new HentPersonQueryRequest();
        request.setIdent(aktørId.getAktørId());

        var projection = new PersonResponseProjection().navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        PersonIdent personIdent = hentPersonidentForAktørId(aktørId).orElseThrow(
            () -> new IllegalStateException("Finner ikke personnummer for id " + aktørId));

        LOG.info("Henter personobjekt");
        var person = pdlKlient.hentPerson(utledYtelse(ytelseType), request, projection);

        var navn = person.getNavn().getFirst();
        return new PersonInfo(navn.getFornavn(), navn.getMellomnavn(), navn.getEtternavn(), personIdent, aktørId, mapFødselsdato(person), null);
    }

    public PersonInfo hentPersonFraIdent(PersonIdent personIdent, Ytelsetype ytelseType) {
        var request = new HentPersonQueryRequest();
        request.setIdent(personIdent.getIdent());

        var projection = new PersonResponseProjection().navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .telefonnummer(new TelefonnummerResponseProjection().landskode().nummer())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        var person = pdlKlient.hentPerson(utledYtelse(ytelseType), request, projection);
        var navn = person.getNavn().getFirst();

        return new PersonInfo(navn.getFornavn(), navn.getMellomnavn(), navn.getEtternavn(), personIdent, null, mapFødselsdato(person),
            mapTelefonnummer(person));
    }

    public PersonIdent finnPersonIdentForAktørId(AktørIdEntitet aktørIdEntitet) {
        return hentPersonidentForAktørId(aktørIdEntitet).orElseThrow(
            () -> new IllegalStateException("Finner ikke personnummer for id " + aktørIdEntitet));
    }

    private LocalDate mapFødselsdato(Person person) {
        return person.getFoedselsdato().stream().map(Foedselsdato::getFoedselsdato).findFirst().map(LocalDate::parse).orElse(null);
    }

    private String mapTelefonnummer(Person person) {
        return person.getTelefonnummer()
            .stream()
            .findFirst()
            .map(telefonnummer -> telefonnummer.getLandskode() + telefonnummer.getNummer())
            .orElse(null);
    }

    private Optional<PersonIdent> hentPersonidentForAktørId(AktørIdEntitet aktørId) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(aktørId.getAktørId());
        request.setGrupper(List.of(IdentGruppe.FOLKEREGISTERIDENT, IdentGruppe.NPID));
        request.setHistorikk(Boolean.FALSE);
        var projection = new IdentlisteResponseProjection().identer(new IdentInformasjonResponseProjection().ident());
        try {
            LOG.info("Henter ident for person");
            var identliste = pdlKlient.hentIdenter(request, projection);
            return identliste.getIdenter().stream().findFirst().map(IdentInformasjon::getIdent).map(PersonIdent::new);
        } catch (VLException v) {
            if (Persondata.PDL_KLIENT_NOT_FOUND_KODE.equals(v.getKode())) {
                LOG.warn("Finner ikke person i PDL, returnerer tomt objekt. Gjelder aktørId {}", aktørId);
                return Optional.empty();
            }
            throw v;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException("FT-723618", "PDL timeout") : e;
        }
    }

    private static Persondata.Ytelse utledYtelse(Ytelsetype ytelseType) {
        if (Ytelsetype.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return Persondata.Ytelse.SVANGERSKAPSPENGER;
        } else {
            return Persondata.Ytelse.FORELDREPENGER;
        }
    }

}
