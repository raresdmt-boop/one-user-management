package ro.mycode.user_management.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "UserSummary",
        description = "Proiecție cu trei campuri, citita direct din baza fara a incarca entitatea")
public interface UserSummary {

    @Schema(description = "Identificatorul utilizatorului",
            example = "3f2a8c14-5b77-4e21-9d3e-8a1c6b0f42de")
    UUID getId();

    @Schema(description = "Prenumele", example = "Cristian")
    String getFirstName();

    @Schema(description = "Emailul", example = "cristian.tudor@gmail.com")
    String getEmail();
}
