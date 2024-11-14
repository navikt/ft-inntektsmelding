package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpplysningerTjenesteTest {

    @Mock
    private PersonTjeneste personTjeneste;

    private OpplysningerTjeneste opplysningerTjeneste;

    @BeforeEach
    void setUp() {
        opplysningerTjeneste = new OpplysningerTjeneste(personTjeneste);
    }

    @Test
    void skal_returnere_tom_innmsender_når_person_ikke_returneres_fra_pdl() {
        when(personTjeneste.hentInnloggetPerson(eq(Ytelsetype.OMSORGSPENGER))).thenReturn(null);

        var resultat = opplysningerTjeneste.hentOpplysninger();

        assertThat(resultat.innsender()).isNull();
    }

    @Test
    void skal_returnere_opplysninger_når_person_finnes() {
        var fornavn = "Test";
        var mellomnavn = "Mellom";
        var etternavn = "Testesen";
        var telefonnummer = "+4799999999";

        var personInfo = new PersonInfo(
            fornavn,
            mellomnavn,
            etternavn,
            new PersonIdent("12345678901"),
            new AktørIdEntitet("2222222222222"),
            null,
            telefonnummer
        );

        when(personTjeneste.hentInnloggetPerson(eq(Ytelsetype.OMSORGSPENGER))).thenReturn(personInfo);

        var resultat = opplysningerTjeneste.hentOpplysninger();

        assertThat(resultat).isNotNull();
        assertThat(resultat.innsender().fornavn()).isEqualTo(fornavn);
        assertThat(resultat.innsender().mellomnavn()).isEqualTo(mellomnavn);
        assertThat(resultat.innsender().etternavn()).isEqualTo(etternavn);
        assertThat(resultat.innsender().telefon()).isEqualTo(telefonnummer);
    }

    @Test
    void skal_returnere_opplysninger_uten_mellomnavn() {
        var fornavn = "Test";
        var etternavn = "Testesen";
        var telefonnummer = "+4799999999";

        var personInfo = new PersonInfo(
            fornavn,
            null,
            etternavn,
            new PersonIdent("12345678901"),
            new AktørIdEntitet("2222222222222"),
            null,
            telefonnummer
        );

        when(personTjeneste.hentInnloggetPerson(eq(Ytelsetype.OMSORGSPENGER))).thenReturn(personInfo);

        var resultat = opplysningerTjeneste.hentOpplysninger();

        assertThat(resultat).isNotNull();
        assertThat(resultat.innsender().fornavn()).isEqualTo(fornavn);
        assertThat(resultat.innsender().mellomnavn()).isNull();
        assertThat(resultat.innsender().etternavn()).isEqualTo(etternavn);
        assertThat(resultat.innsender().telefon()).isEqualTo(telefonnummer);
    }
}
