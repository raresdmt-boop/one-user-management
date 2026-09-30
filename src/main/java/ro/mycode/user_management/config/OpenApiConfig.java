package ro.mycode.user_management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Management API")
                        .version("1.0.0")
                        .description("""
                                API de gestiune a utilizatorilor, folosit ca material de curs.

                                ## Convenția de statusuri

                                Statusul descrie rezultatul transferului, nu ruta apelată.

                                | Situație | Status |
                                |---|---|
                                | resursă creată | `201 Created`, cu header `Location` |
                                | orice alt succes | `200 OK`, cu reprezentare în corp |
                                | cerere invalidă | `400 Bad Request` |
                                | id sau email inexistent | `404 Not Found` |
                                | email deja folosit | `409 Conflict` |

                                O colecție goală răspunde `200` cu `[]`, iar o medie fără date \
                                răspunde `200` cu `{"averageAge": null}` — lipsa datelor este un \
                                rezultat, nu o eroare.

                                ## Erori

                                Orice răspuns de eroare are forma `ApiError`. Erorile de validare \
                                poartă în plus lista `details`, cu un rând per câmp respins.""")
                        .contact(new Contact().name("MyCodeSchool").url("https://mycodeschool.ro"))
                        .license(new License().name("Material de curs")))
                .servers(List.of(new Server().url("/").description("Serverul curent")));
    }
}
