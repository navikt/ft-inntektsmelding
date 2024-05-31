package no.nav.familie.inntektsmelding.typer;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record AktørIdDto(@NotNull @Digits(integer = 19, fraction = 0) String id) {
    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + maskerAktørId() + ">";
    }

    private String maskerAktørId() {
        if (id == null) {
            return "";
        }
        var length = id.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + id.substring(length - 4);
    }
}
