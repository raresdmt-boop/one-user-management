package ro.mycode.user_management.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Schema(name = "UserUpdateRequest",
        description = "Modificare parțiala: toate campurile sunt optionale, iar cele lipsa sau "
                + "goale lasa valoarea veche neatinsa. Un corp gol `{}` este o cerere valida "
                + "care nu schimba nimic.")
public record UserUpdateRequest(

        @Schema(description = "Prenumele nou; null sau gol inseamna nemodificat", example = "Cristi")
        String firstName,

        @Schema(description = "Numele nou; null sau gol inseamna nemodificat", example = "Tudor")
        String lastName,

        @Schema(description = "Emailul nou; daca aparține altui utilizator, cererea primeste 409",
                example = "cristi.tudor@gmail.com")
        @Email(message = "Email must be a valid address")
        String email,

        @Schema(description = "Varsta noua, intre 1 si 120; null inseamna nemodificata",
                example = "31", minimum = "1", maximum = "120")
        @Positive(message = "Age must be greater than zero")
        @Max(value = 120, message = "Age must be at most 120")
        Integer age) {
}
