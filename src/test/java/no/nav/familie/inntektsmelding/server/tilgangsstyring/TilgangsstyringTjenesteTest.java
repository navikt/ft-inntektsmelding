package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.pip.PipTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.BasisKontekst;
import no.nav.vedtak.sikkerhet.kontekst.Groups;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class TilgangsstyringTjenesteTest {

    private Tilgang tilgangsstyringTjeneste;

    @Mock
    private PipTjeneste pipTjeneste;

    @Mock
    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    @BeforeEach
    void setUp() {
        tilgangsstyringTjeneste = new TilgangsstyringTjeneste(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_uten_request_kontekts_nok() {
        try (var mockedKontekst = Mockito.mockStatic(KontekstHolder.class)) {
            mockedKontekst.when(KontekstHolder::getKontekst).thenReturn(BasisKontekst.ikkeAutentisertRequest("testConsument"));

            var ex = assertThrows(ManglerTilgangException.class,
                () -> tilgangsstyringTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(UUID.randomUUID()));
            assertThat(ex.getMessage()).contains("Kun borger kall støttes.");
        }
        verifyNoInteractions(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_uten_riktig_token_type_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.InternBruker));
            var ex = assertThrows(ManglerTilgangException.class,
                () -> tilgangsstyringTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(UUID.randomUUID()));
            assertThat(ex.getMessage()).contains("Kun borger kall støttes.");
        }
        verifyNoInteractions(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_informasjon_om_bedrift_fra_pip_forespørsel_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            var forespørselUuid = UUID.randomUUID();
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.EksternBruker));
            var ex = assertThrows(ManglerTilgangException.class,
                () -> {
                    tilgangsstyringTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);
                });
            assertThat(ex.getMessage()).contains("Mangler informasjon om bedrift.");

            verify(pipTjeneste).hentOrganisasjonsnummerFor(forespørselUuid);
        }
        verifyNoInteractions(altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_tilgang_til_tjenesten_fra_pip_forespørsel_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            var forespørselUuid = UUID.randomUUID();
            var fakeOrgNr = "123456789";
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.EksternBruker));
            when(pipTjeneste.hentOrganisasjonsnummerFor(forespørselUuid)).thenReturn(new OrganisasjonsnummerDto(fakeOrgNr));
            when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(true);

            var ex = assertThrows(ManglerTilgangException.class,
                () -> tilgangsstyringTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));
            assertThat(ex.getMessage()).contains("Bruker mangler tilgang til bedriften i Altinn.");

            verify(pipTjeneste).hentOrganisasjonsnummerFor(forespørselUuid);
            verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
        }
    }

    @Test
    void test_borgen_inisjert_kall_fra_pip_forespørsel_tilgang_ok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            var forespørselUuid = UUID.randomUUID();
            var fakeOrgNr = "123456789";
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.EksternBruker));
            when(pipTjeneste.hentOrganisasjonsnummerFor(forespørselUuid)).thenReturn(new OrganisasjonsnummerDto(fakeOrgNr));
            when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(false);

            assertDoesNotThrow(() -> tilgangsstyringTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));

            verify(pipTjeneste).hentOrganisasjonsnummerFor(forespørselUuid);
            verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
        }
    }


    @Test
    void test_borgen_inisjert_kall_mangler_informasjon_om_bedrift_fra_pip_inntektsmelding_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            var inntektsmeldingId = 1L;
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.EksternBruker));
            var ex = assertThrows(ManglerTilgangException.class,
                () -> tilgangsstyringTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));
            assertThat(ex.getMessage()).contains("Mangler informasjon om bedrift.");

            verify(pipTjeneste).hentOrganisasjonsnummerFor(inntektsmeldingId);
        }
        verifyNoInteractions(altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_tilgang_til_tjenesten_fra_pip_inntektsmelding_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            var inntektsmeldingId = 1L;
            var fakeOrgNr = "123456789";
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.EksternBruker));
            when(pipTjeneste.hentOrganisasjonsnummerFor(inntektsmeldingId)).thenReturn(new OrganisasjonsnummerDto(fakeOrgNr));
            when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(true);

            var ex = assertThrows(ManglerTilgangException.class,
                () -> tilgangsstyringTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));
            assertThat(ex.getMessage()).contains("Bruker mangler tilgang til bedriften i Altinn.");

            verify(pipTjeneste).hentOrganisasjonsnummerFor(inntektsmeldingId);
            verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
        }
    }

    @Test
    void test_borgen_inisjert_kall_fra_pip_inntektsmelding_tilgang_ok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            var inntektsmeldingId = 1L;
            var fakeOrgNr = "123456789";
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.EksternBruker));
            when(pipTjeneste.hentOrganisasjonsnummerFor(inntektsmeldingId)).thenReturn(new OrganisasjonsnummerDto(fakeOrgNr));
            when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(false);

            assertDoesNotThrow(() -> tilgangsstyringTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));

            verify(pipTjeneste).hentOrganisasjonsnummerFor(inntektsmeldingId);
            verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
        }
    }

    @Test
    void test_sjekk_om_saksbehandler_har_rollen_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.EksternBruker));

            var ex = assertThrows(ManglerTilgangException.class, () -> tilgangsstyringTjeneste.sjekkAtSaksbehandlerHarRollenDrift());
            assertThat(ex.getMessage()).contains("Saksbehandler mangler en rolle.");
        }
    }

    @Test
    void test_sjekk_om_saksbehandler_har_rollen_ikke_saksbehandler_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.EksternBruker));

            var ex = assertThrows(ManglerTilgangException.class, () -> tilgangsstyringTjeneste.sjekkAtSaksbehandlerHarRollenDrift());
            assertThat(ex.getMessage()).contains("Saksbehandler mangler en rolle.");
        }
    }

    @Test
    void test_sjekk_om_saksbehandler_har_rollen_mangler_rollen_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.InternBruker));

            var ex = assertThrows(ManglerTilgangException.class, () -> tilgangsstyringTjeneste.sjekkAtSaksbehandlerHarRollenDrift());
            assertThat(ex.getMessage()).contains("Saksbehandler mangler en rolle.");
        }
    }

    @Test
    void test_sjekk_om_saksbehandler_har_rollen_feil_rolle_nok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            mockedKontekts.when(KontekstHolder::getKontekst)
                .thenReturn(fakeRequestKontekts(IdentType.InternBruker, Set.of(Groups.SAKSBEHANDLER, Groups.VEILEDER)));

            var ex = assertThrows(ManglerTilgangException.class, () -> tilgangsstyringTjeneste.sjekkAtSaksbehandlerHarRollenDrift());
            assertThat(ex.getMessage()).contains("Saksbehandler mangler en rolle.");
        }
    }

    @Test
    void test_sjekk_om_saksbehandler_har_rollen_ok() {
        try (var mockedKontekts = Mockito.mockStatic(KontekstHolder.class)) {
            var forventetRolle = Groups.DRIFT;
            mockedKontekts.when(KontekstHolder::getKontekst).thenReturn(fakeRequestKontekts(IdentType.InternBruker, Set.of(forventetRolle)));
            assertDoesNotThrow(() -> tilgangsstyringTjeneste.sjekkAtSaksbehandlerHarRollenDrift());
        }
    }

    private Kontekst fakeRequestKontekts(IdentType identType) {
        return fakeRequestKontekts(identType, Set.of());
    }

    private Kontekst fakeRequestKontekts(IdentType identType, Set<Groups> groups) {
        return RequestKontekst.forRequest("brukerUid", "brukerUid", identType, fakeOidcToken(OpenIDProvider.TOKENX), groups);
    }

    private OpenIDToken fakeOidcToken(OpenIDProvider provider) {
        return new OpenIDToken(provider, new TokenString("fakeTokenString"));
    }
}
