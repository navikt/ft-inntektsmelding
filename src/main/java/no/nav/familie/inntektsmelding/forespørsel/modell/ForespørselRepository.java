package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Dependent
public class ForespørselRepository {

    private EntityManager entityManager;
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRepository.class);

    public ForespørselRepository() {
    }

    @Inject
    public ForespørselRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public UUID lagreForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnummer, String fagsakSaksnummer) {
        var forespørselEntitet = new ForespørselEntitet(orgnummer, skjæringstidspunkt, new AktørIdEntitet(aktørId), ytelsetype, fagsakSaksnummer);

        final Query query = entityManager.createNativeQuery("SELECT nextval('SEQ_FORESPOERSEL')");
        var tall = (Number) query.getSingleResult();

        LOG.info("ForespørselRepository: lagrer forespørsel entitet: {} med id:{}", forespørselEntitet, tall);

        entityManager.persist(forespørselEntitet);
        entityManager.flush();
        return forespørselEntitet.getUuid();
    }

    public void oppdaterOppgaveId(UUID forespørselUUID, String oppgaveId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setOppgaveId(oppgaveId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public void oppdaterArbeidsgiverNotifikasjonSakId(UUID forespørselUUID, String arbeidsgiverNotifikasjonSakId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setArbeidsgiverNotifikasjonSakId(arbeidsgiverNotifikasjonSakId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID uuid) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where uuid = :foresporselUUID", ForespørselEntitet.class)
            .setParameter("foresporselUUID", uuid);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException("Forventet å finne kun en forespørsel for oppgitt uuid " + uuid);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public void ferdigstillForespørsel(String arbeidsgiverNotifikasjonSakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :SAK_ID", ForespørselEntitet.class)
            .setParameter("SAK_ID", arbeidsgiverNotifikasjonSakId);
        var resultList = query.getResultList();

        LOG.debug("Fant entiteter for ferdigstilling: {}", resultList);

        resultList.forEach(f -> {
            f.setStatus(ForespørselStatus.FERDIG);
            entityManager.persist(f);
        });

        entityManager.flush();
    }

    public void settForespørselTilUtgått(String arbeidsgiverNotifikasjonSakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :SAK_ID", ForespørselEntitet.class)
            .setParameter("SAK_ID", arbeidsgiverNotifikasjonSakId);
        var resultList = query.getResultList();

        resultList.forEach(f -> {
            f.setStatus(ForespørselStatus.UTGÅTT);
            entityManager.persist(f);
        });

        entityManager.flush();
    }


    public List<ForespørselEntitet> hentForespørsler(SaksnummerDto fagsakSaksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet f where fagsystemSaksnummer = :saksnr", ForespørselEntitet.class)
            .setParameter("saksnr", fagsakSaksnummer.saksnr());
        return query.getResultList();
    }


    public Optional<ForespørselEntitet> finnForespørsel(AktørIdEntitet aktørId, ArbeidsgiverDto arbeidsgiverIdent, LocalDate startdato) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where aktørId = :brukerAktørId and organisasjonsnummer = :arbeidsgiverIdent "
                + "and skjæringstidspunkt = :skjæringstidspunkt", ForespørselEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent.ident())
            .setParameter("skjæringstidspunkt", startdato);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            var feilmelding = String.format("Forventet å finne kun en forespørsel for gitt aktør %s, arbeidsgiver %s og startdato %s", aktørId,
                arbeidsgiverIdent, startdato);
            throw new IllegalStateException(feilmelding);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public Optional<ForespørselEntitet> finnÅpenForespørsel(AktørIdEntitet aktørId,
                                                            Ytelsetype ytelsetype,
                                                            String arbeidsgiverIdent,
                                                            LocalDate startdato) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where status='UNDER_BEHANDLING' " + "and aktørId = :brukerAktørId "
                    + "and organisasjonsnummer = :arbeidsgiverIdent " + "and skjæringstidspunkt = :skjæringstidspunkt " + "and ytelseType = :ytelsetype",
                ForespørselEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("skjæringstidspunkt", startdato)
            .setParameter("ytelsetype", ytelsetype);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException(
                "Forventet å finne kun en forespørsel for gitt id arbeidsgiver og startdato" + aktørId + arbeidsgiverIdent + startdato);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public List<ForespørselEntitet> finnÅpenForespørsel(SaksnummerDto fagsystemSaksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where status=:status " + "and fagsystemSaksnummer=:saksnummer",
                ForespørselEntitet.class)
            .setParameter("saksnummer", fagsystemSaksnummer.saksnr())
            .setParameter("status", ForespørselStatus.UNDER_BEHANDLING);
        return query.getResultList();
    }
}
