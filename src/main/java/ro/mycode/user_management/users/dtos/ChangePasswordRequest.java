package ro.mycode.user_management.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ChangePasswordRequest", description = "Parola noua a utilizatorului")
public record ChangePasswordRequest(

        @Schema(description = "Parola noua, cel puțin 8 caractere",
                example = "parolaNoua1", minLength = 8, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword) {
}
