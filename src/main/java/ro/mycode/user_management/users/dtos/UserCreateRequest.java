package ro.mycode.user_management.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "UserCreateRequest", description = "Datele necesare la crearea unui utilizator. "
        + "Toate campurile sunt obligatorii.")
public record UserCreateRequest(

        @Schema(description = "Prenumele", example = "Cristian", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "Numele de familie", example = "Tudor", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        String lastName,

        @Schema(description = "Emailul; daca exista deja, cererea primeste 409",
                example = "cristian.tudor@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @Schema(description = "Parola, cel puțin 8 caractere; nu este niciodata intoarsa de API",
                example = "parola123", minLength = 8, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(description = "Varsta, intre 1 si 120", example = "30",
                minimum = "1", maximum = "120", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive(message = "Age must be greater than zero")
        @Max(value = 120, message = "Age must be at most 120")
        int age) {
}
