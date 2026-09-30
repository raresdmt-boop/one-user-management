package ro.mycode.user_management.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ApiError", description = "Forma unică a oricărui răspuns de eroare")
public record ApiError(

        @Schema(description = "Momentul in care serverul a construit raspunsul",
                example = "2026-09-30T21:15:42.113Z")
        Instant timestamp,

        @Schema(description = "Codul HTTP, repetat in corp pentru clientii care logheaza doar corpul",
                example = "400")
        int status,

        @Schema(description = "Denumirea standard a codului HTTP", example = "Bad Request")
        String error,

        @Schema(description = "Ce s-a intamplat, intr-o propozitie", example = "Validation failed")
        String message,

        @Schema(description = "Calea ceruta, ca sa se distinga o ruta greșita de o resursa lipsa",
                example = "/api/users")
        String path,

        @Schema(description = "Un rand per camp respins, sortat alfabetic; lista este goala "
                + "pentru erorile care nu vin din validare",
                example = "[\"age: Age must be greater than zero\"]")
        List<String> details) {
}
