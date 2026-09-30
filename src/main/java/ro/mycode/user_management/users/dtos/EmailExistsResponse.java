package ro.mycode.user_management.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EmailExistsResponse",
        description = "Raspunsul la intrebarea daca un email este deja folosit. "
                + "Un email inexistent NU este o eroare: raspunsul este 200 cu `exists: false`.")
public record EmailExistsResponse(

        @Schema(description = "Emailul cautat, repetat ca raspunsul sa se poata citi singur",
                example = "cristian.tudor@gmail.com")
        String email,

        @Schema(description = "Daca emailul exista in sistem", example = "true")
        boolean exists) {
}
