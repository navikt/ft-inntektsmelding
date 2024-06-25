package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import no.nav.familie.inntektsmelding.koder.SakStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Entity(name = "SakEntitet")
@Table(name = "SAK")
public class SakEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAK")
    private Long id;

    @Column(name = "gruppering_uuid", nullable = false, updatable = false)
    private UUID grupperingUuid;

    @Column(name = "FAGER_SAK_ID")
    private String fagerSakId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sak_status", nullable = false)
    private SakStatus sakStatus = SakStatus.UNDER_BEHANDLING;

    @Column(name = "orgnr", nullable = false, updatable = false)
    private String organisasjonsnummer;

    @Embedded
    private AktørIdEntitet aktørId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private Ytelsetype ytelseType;

    @Column(name = "fagsystem_saksnummer", nullable = false, updatable = false)
    private String fagsystemSaksnummer;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    public SakEntitet(String organisasjonsnummer,
                      AktørIdEntitet aktørId,
                      Ytelsetype ytelseType,
                      String fagsystemSaksnummer) {
        this.grupperingUuid = UUID.randomUUID();
        this.organisasjonsnummer = organisasjonsnummer;
        this.aktørId = aktørId;
        this.ytelseType = ytelseType;
        this.fagsystemSaksnummer = fagsystemSaksnummer;
    }

    public SakEntitet() {
    }

    @PreUpdate
    protected void onUpdate() {
        endretTidspunkt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }


    public String getFagerSakId() {
        return fagerSakId;
    }

    public void setFagerSakId(String fagerSakId) {
        this.fagerSakId = fagerSakId;
    }


    public SakStatus getSakStatus() {
        return sakStatus;
    }

    public void setSakStatus(SakStatus sakStatus) {
        this.sakStatus = sakStatus;
    }


    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }


    public AktørIdEntitet getAktørId() {
        return aktørId;
    }

    public Ytelsetype getYtelseType() {
        return ytelseType;
    }

    public String getFagsystemSaksnummer() {
        return fagsystemSaksnummer;
    }

    public UUID getGrupperingUuid() {
        return grupperingUuid;
    }
}
