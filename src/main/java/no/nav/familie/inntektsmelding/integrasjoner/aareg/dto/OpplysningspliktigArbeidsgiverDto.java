package no.nav.familie.inntektsmelding.integrasjoner.aareg.dto;

public record OpplysningspliktigArbeidsgiverDto(Type type, String organisasjonsnummer, String aktoerId, String offentligIdent) {

    @Override
    public String toString() {
        return "OpplysningspliktigArbeidsgiverDto{" + "type=" + type + ", organisasjonsnummer='" + organisasjonsnummer + '\'' + ", aktoerId='"
            + aktoerId + '\'' + ", offentligIdent='" + offentligIdent + '\'' + '}';
    }

    public enum Type {
        Organisasjon,
        Person
    }
}
