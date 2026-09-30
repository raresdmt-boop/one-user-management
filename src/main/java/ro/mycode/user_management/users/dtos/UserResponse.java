package ro.mycode.user_management.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import ro.mycode.user_management.users.models.User;

import java.util.UUID;

@Schema(name = "UserResponse",
        description = "Reprezentarea unui utilizator. Este singura forma intoarsa de API, "
                + "la creare, citire, modificare, schimbare de parola si stergere. "
                + "Nu conține niciodata parola.")
public record UserResponse(

        @Schema(description = "Identificatorul generat la creare",
                example = "3f2a8c14-5b77-4e21-9d3e-8a1c6b0f42de")
        UUID id,

        @Schema(description = "Prenumele", example = "Cristian")
        String firstName,

        @Schema(description = "Numele de familie", example = "Tudor")
        String lastName,

        @Schema(description = "Emailul, unic in sistem", example = "cristian.tudor@gmail.com")
        String email,

        @Schema(description = "Varsta, intre 1 si 120", example = "30")
        int age) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAge());
    }
}
