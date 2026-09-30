package ro.mycode.user_management.users.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AverageAgeResponse",
        description = "Media varstelor. Pe o baza goala campul este `null`, nu zero, si statusul "
                + "rămâne 200 — lipsa datelor este un rezultat, nu o eroare.")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AverageAgeResponse(

        @Schema(description = "Media varstelor, sau null daca nu exista niciun utilizator",
                example = "26.6", nullable = true)
        Double averageAge) {
}
