package ro.mycode.user_management.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserCountResponse", description = "Cati utilizatori sunt sub o varsta data")
public record UserCountResponse(

        @Schema(description = "Limita ceruta, exclusiva", example = "30")
        int youngerThan,

        @Schema(description = "Cati utilizatori sunt strict sub limita", example = "3")
        long count) {
}
