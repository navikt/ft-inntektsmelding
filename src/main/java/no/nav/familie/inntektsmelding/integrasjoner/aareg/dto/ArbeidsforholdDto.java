package no.nav.familie.inntektsmelding.integrasjoner.aareg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record ArbeidsforholdDto(
    @JsonProperty("arbeidsforholdId")
    String arbeidsforholdId,
    
    @JsonProperty("navArbeidsforholdId")
    Long navArbeidsforholdId,
    
    @JsonProperty("arbeidsgiver")
    OpplysningspliktigArbeidsgiverDto arbeidsgiver,
    
    @JsonProperty("ansettelsesperiode")
    AnsettelsesperiodeDto ansettelsesperiode,
    
    @JsonProperty("arbeidsavtaler")
    List<ArbeidsavtaleDto> arbeidsavtaler,
    
    @JsonProperty("permisjonPermitteringer")
    List<PermisjonPermitteringDto> permisjonPermitteringer,
    
    @JsonProperty("type")
    String type // (kodeverk: Arbeidsforholdtyper)
) {
    public List<ArbeidsavtaleDto> arbeidsavtaler() {
        return arbeidsavtaler != null ? arbeidsavtaler : List.of();
    }

    public List<PermisjonPermitteringDto> permisjonPermitteringer() {
        return permisjonPermitteringer != null ? permisjonPermitteringer : List.of();
    }
}
