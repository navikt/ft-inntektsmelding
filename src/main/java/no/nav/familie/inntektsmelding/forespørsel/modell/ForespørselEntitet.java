package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Sak;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.SakStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Entity(name = "ForespørselEntitet")
@Table(name = "FORESPOERSEL")
public class ForespørselEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FORESPOERSEL")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "INTERN_SAK_ID", nullable = false, updatable = false)
    private SakEntitet sak;

    @Enumerated(EnumType.STRING)
    @Column(name = "FORESPOERSEL_STATUS", nullable = false, updatable = false)
    private ForespørselStatus forespørselStatus = ForespørselStatus.NY;

    @Column(name = "oppgave_id")
    private String oppgaveId;

    @Column(name = "skjaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    public ForespørselEntitet(SakEntitet sakEntitet, LocalDate skjæringstidspunkt) {
        this.uuid = UUID.randomUUID();
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.sak = sakEntitet;
    }

    public ForespørselEntitet() {
    }

    @PreUpdate
    protected void onUpdate() {
        endretTidspunkt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }



    public String getOppgaveId() {
        return oppgaveId;
    }

    void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
    }


    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public ForespørselStatus getForespørselStatus() {
        return forespørselStatus;
    }

    void setForespørselStatus(ForespørselStatus forespørselStatus) {
        this.forespørselStatus = forespørselStatus;
    }

    public SakEntitet getSak() {
        return sak;
    }

    void setSak(SakEntitet sak) {
        this.sak = sak;
    }
}
