package no.nav.familie.inntektsmelding.integrasjoner.aareg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsforholdDto {

    @JsonProperty("arbeidsforholdId")
    private String arbeidsforholdId;
    @JsonProperty("navArbeidsforholdId")
    private Long navArbeidsforholdId;
    @JsonProperty("arbeidsgiver")
    private OpplysningspliktigArbeidsgiverDto arbeidsgiver;
    @JsonProperty("ansettelsesperiode")
    private AnsettelsesperiodeDto ansettelsesperiode;
    @JsonProperty("arbeidsavtaler")
    private List<ArbeidsavtaleDto> arbeidsavtaler;
    @JsonProperty("permisjonPermitteringer")
    private List<PermisjonPermitteringDto> permisjonPermitteringer;
    @JsonProperty("type")
    private String type; // (kodeverk: Arbeidsforholdtyper)

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public Long getNavArbeidsforholdId() {
        return navArbeidsforholdId;
    }

    public OpplysningspliktigArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public AnsettelsesperiodeDto getAnsettelsesperiode() {
        return ansettelsesperiode;
    }

    public List<ArbeidsavtaleDto> getArbeidsavtaler() {
        return arbeidsavtaler != null ? arbeidsavtaler : List.of();
    }

    public List<PermisjonPermitteringDto> getPermisjonPermitteringer() {
        return permisjonPermitteringer != null ? permisjonPermitteringer : List.of();
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ArbeidsforholdDto{" + "arbeidsforholdId='" + arbeidsforholdId + '\'' + ", navArbeidsforholdId=" + navArbeidsforholdId
            + ", arbeidsgiver=" + arbeidsgiver + ", ansettelsesperiode=" + ansettelsesperiode + ", arbeidsavtaler=" + arbeidsavtaler
            + ", permisjonPermitteringer=" + permisjonPermitteringer + ", type='" + type + '\'' + '}';
    }
}
