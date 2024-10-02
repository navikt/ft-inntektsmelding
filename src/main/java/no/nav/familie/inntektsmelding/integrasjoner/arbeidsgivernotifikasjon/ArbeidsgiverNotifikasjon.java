package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.OffsetDateTime;

public interface ArbeidsgiverNotifikasjon {

    String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke);

    HentetSak hentSakMedGrupperingsid(String grupperingsid, Merkelapp merkelapp);

    HentetSak hentSak(String sakId);

    String oppdaterSakStatus(String sakId, SaksStatus status, String overstyrtStatusText);

    String oppdaterSakStatusMedGrupperingsId(String grupperingsid, Merkelapp merkelapp, SaksStatus status, String overstyrtStatusText);


    String opprettOppgave(String grupperingsid,
                          Merkelapp merkelapp,
                          String eksternId,
                          String virksomhetsnummer,
                          String notifikasjonsTekst,
                          URI lenke);

    String ferdigstillSak(String id);

    String oppgaveUtfoert(String oppgaveId, OffsetDateTime utfoertTidspunkt);

    String oppgaveUtfoertByEksternId(String eksternId, Merkelapp merkelapp, OffsetDateTime tidspunkt);

    String oppgaveUtgaatt(String oppgaveId, OffsetDateTime utgaattTidspunkt);

}
