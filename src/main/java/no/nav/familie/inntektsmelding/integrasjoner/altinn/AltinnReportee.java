package no.nav.familie.inntektsmelding.integrasjoner.altinn;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
record AltinnReportee(String name,
                      String organizationForm,
                      String organizationNumber,
                      String parentOrganizationNumber,
                      String socialSecurityNumber,
                      String status,
                      String type) {
}
