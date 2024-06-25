package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.SakStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Dependent
public class ForespørselOgSakRepository {

    private EntityManager entityManager;

    public ForespørselOgSakRepository() {
    }

    @Inject
    public ForespørselOgSakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public SakEntitet opprettSak(Ytelsetype ytelsetype, String aktørId, String orgnummer, String fagsakSaksnummer) {
        var sakEntitet = new SakEntitet(orgnummer, new AktørIdEntitet(aktørId), ytelsetype, fagsakSaksnummer);
        entityManager.persist(sakEntitet);
        entityManager.flush();
        return sakEntitet;
    }

    public UUID lagreForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnummer, String fagsakSaksnummer) {
        var sak = finnSakUnderBehandling(new AktørIdEntitet(aktørId), ytelsetype, orgnummer, fagsakSaksnummer);
        if (sak.isEmpty()) {
            throw new IllegalStateException("Kan ikke opprette forespørsel uten å ha en åpen sak");
        }

        var forespørselEntitet = new ForespørselEntitet(sak.get(), skjæringstidspunkt);

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

    public void oppdaterSakId(Long internSakId, String sakId) {
        var sak = hentSak(internSakId);
        sak.setFagerSakId(sakId);
        entityManager.persist(sak);
        entityManager.flush();
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

    public void ferdigstillSak(Long internSakId) {
        var resultat = hentSak(internSakId);
        resultat.setSakStatus(SakStatus.FERDIG);
        entityManager.persist(resultat);
        entityManager.flush();
    }

    public SakEntitet hentSak(Long internSakId) {
        var query = entityManager.createQuery("FROM SakEntitet where id = :internSakId", SakEntitet.class).setParameter("internSakId", internSakId);
        var resultat = query.getSingleResult();
        return resultat;
    }

    public void utførForespørsel(UUID forespørselUUID) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where uuid = :forespørselUUID", ForespørselEntitet.class)
            .setParameter("forespørselUUID", forespørselUUID);
        var resultat = query.getSingleResult();
        resultat.setForespørselStatus(ForespørselStatus.UTFOERT);
        entityManager.persist(resultat);
        entityManager.flush();
    }

    public Optional<ForespørselEntitet> finnÅpenForespørsel(AktørIdEntitet aktørId,
                                                            Ytelsetype ytelsetype,
                                                            String arbeidsgiverIdent,
                                                            String fagsakSaksnummer,
                                                            LocalDate startdato) {
        var query = entityManager.createQuery(
                "FROM ForespørselEntitet where forespørselStatus=:status " +
                    "and skjæringstidspunkt = :startdato " +
                    "and sak.aktørId = :brukerAktørId " +
                    "and sak.organisasjonsnummer = :arbeidsgiverIdent " +
                    "and sak.fagsystemSaksnummer = :fagsakSaksnummer " +
                    "and sak.ytelseType = :ytelsetype", ForespørselEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("fagsakSaksnummer", fagsakSaksnummer)
            .setParameter("ytelsetype", ytelsetype)
            .setParameter("startdato", startdato)
            .setParameter("status", ForespørselStatus.NY);


        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException("Forventet å finne kun en forespørsel");
        } else {
            return Optional.of(resultList.getFirst());
        }
    }


    public Optional<SakEntitet> finnSakUnderBehandling(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String arbeidsgiverIdent, String fagsakSaksnummer) {
        var query = entityManager.createQuery(
                "FROM SakEntitet where sakStatus= :status " + "and aktørId = :brukerAktørId " + "and organisasjonsnummer = :arbeidsgiverIdent "
                    + "and fagsystemSaksnummer = :fagsakSaksnummer " + "and ytelseType = :ytelsetype", SakEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("fagsakSaksnummer", fagsakSaksnummer)
            .setParameter("ytelsetype", ytelsetype)
            .setParameter("status", SakStatus.UNDER_BEHANDLING);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException("Forventet å finne kun en sak for gitt id arbeidsgiver og startdato");
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

}
