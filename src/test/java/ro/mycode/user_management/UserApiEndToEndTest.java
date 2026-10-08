package ro.mycode.user_management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import ro.mycode.user_management.users.repository.UserRepository;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:e2e_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@DisplayName("API useri (end-to-end) - server pornit pe port real, cereri HTTP adevarate")
class UserApiEndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private RestTestClient client;

    @BeforeEach
    void startClientOnEmptyDatabase() {
        // Arrange
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        userRepository.deleteAll();
    }

    private static String createBody(String firstName, String lastName, String email, int age) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "email": "%s",
                  "password": "parola123",
                  "age": %d
                }
                """.formatted(firstName, lastName, email, age);
    }

    private URI register(String firstName, String lastName, String email, int age) {
        return client.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createBody(firstName, lastName, email, age))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(String.class)
                .getResponseHeaders().getLocation();
    }

    @Nested
    @DisplayName("Scenarii de utilizare, cap-coada, prin retea")
    class Scenarios {

        @Test
        @DisplayName("Location intors la creare este o adresa absoluta pe portul real si se poate urma direct")
        void createdLocation_isAbsoluteAndFollowable() {
            // Act
            URI location = register("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);

            // Assert
            assertThat(location).isNotNull();
            assertThat(location.getPort()).isEqualTo(port);
            assertThat(location.getPath()).startsWith("/api/users/");

            client.get().uri(location)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.email").isEqualTo("cristian.tudor@gmail.com")
                    .jsonPath("$.age").isEqualTo(30);
        }

        @Test
        @DisplayName("un user se inregistreaza, e gasit, isi schimba datele si parola, apoi pleaca")
        void userJourney_fromRegistrationToDeletion() {
            // Arrange
            URI location = register("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);

            // Act & Assert
            client.get().uri("/api/users/exists?email=cristian.tudor@gmail.com")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.exists").isEqualTo(true);

            client.patch().uri(location)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"email\":\"cristi.tudor@gmail.com\",\"age\":31}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.email").isEqualTo("cristi.tudor@gmail.com")
                    .jsonPath("$.firstName").isEqualTo("Cristian");

            client.get().uri("/api/users/by-email?email=cristi.tudor@gmail.com")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.age").isEqualTo(31);

            client.put().uri(location + "/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"newPassword\":\"parolaNoua1\"}")
                    .exchange()
                    .expectStatus().isOk();

            client.delete().uri(location)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.email").isEqualTo("cristi.tudor@gmail.com");

            client.get().uri(location)
                    .exchange()
                    .expectStatus().isNotFound();

            client.get().uri("/api/users/exists?email=cristi.tudor@gmail.com")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.exists").isEqualTo(false);
        }

        @Test
        @DisplayName("cautarea paginata parcurge toate paginile fara sa piarda sau sa repete pe cineva")
        void pagedSearch_walksEveryPageExactlyOnce() {
            // Arrange
            register("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);
            register("Bogdan", "Alexandrescu", "bogdan.alexandrescu@gmail.com", 41);
            register("Ana", "Stefanescu", "ana.stefanescu@yahoo.com", 19);
            register("Radu", "Popescu", "radu.popescu@gmail.com", 17);
            register("Maria", "Stere", "maria.stere@gmail.com", 26);

            // Act & Assert
            client.get().uri("/api/users/search?size=2&page=0")
                    .exchange()
                    .expectBody()
                    .jsonPath("$.content[0].lastName").isEqualTo("Alexandrescu")
                    .jsonPath("$.content[1].lastName").isEqualTo("Popescu")
                    .jsonPath("$.last").isEqualTo(false);

            client.get().uri("/api/users/search?size=2&page=1")
                    .exchange()
                    .expectBody()
                    .jsonPath("$.content[0].lastName").isEqualTo("Stefanescu")
                    .jsonPath("$.content[1].lastName").isEqualTo("Stere")
                    .jsonPath("$.last").isEqualTo(false);

            client.get().uri("/api/users/search?size=2&page=2")
                    .exchange()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(1)
                    .jsonPath("$.content[0].lastName").isEqualTo("Tudor")
                    .jsonPath("$.last").isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Ce ajunge efectiv pe fir")
    class OnTheWire {

        @Test
        @DisplayName("parola nu apare in niciun raspuns, nici macar ca text in corp")
        void password_neverLeavesTheServer() {
            // Arrange
            URI location = register("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);

            // Act
            String created = client.get().uri(location).exchange()
                    .returnResult(String.class).getResponseBody();
            String changed = client.put().uri(location + "/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"newPassword\":\"parolaNoua1\"}")
                    .exchange()
                    .returnResult(String.class).getResponseBody();
            String all = client.get().uri("/api/users").exchange()
                    .returnResult(String.class).getResponseBody();

            // Assert
            assertThat(created).doesNotContain("password").doesNotContain("parola123");
            assertThat(changed).doesNotContain("password").doesNotContain("parolaNoua1");
            assertThat(all).doesNotContain("password");
        }

        @Test
        @DisplayName("average-age pe baza goala trimite null explicit, nu un camp lipsa")
        void averageAge_sendsExplicitNullOnEmptyDatabase() {
            // Act & Assert
            client.get().uri("/api/users/average-age")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().json("{\"averageAge\":null}");
        }

        @Test
        @DisplayName("documentatia OpenAPI si Swagger UI se servesc de serverul pornit")
        void apiDocsAndSwaggerUi_areServed() {
            // Act & Assert
            client.get().uri("/v3/api-docs")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.info.title").isEqualTo("User Management API");

            client.get().uri("/swagger-ui/index.html")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
        }
    }

    @Nested
    @DisplayName("Erorile au aceeasi forma ApiError si prin serverul real")
    class Errors {

        @Test
        @DisplayName("400 pe validare listeaza fiecare camp respins si calea ceruta")
        void validationError_hasFullApiErrorShape() {
            // Act & Assert
            client.post().uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createBody("", "Tudor", "nu-e-email", 0))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.timestamp").exists()
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.error").isEqualTo("Bad Request")
                    .jsonPath("$.message").isEqualTo("Validation failed")
                    .jsonPath("$.path").isEqualTo("/api/users")
                    .jsonPath("$.details.length()").isEqualTo(3);

            assertThat(userRepository.count()).isZero();
        }

        @Test
        @DisplayName("JSON stricat primeste 400, nu 500")
        void malformedJson_returns400() {
            // Act & Assert
            client.post().uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"firstName\": ")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.message").isEqualTo("Malformed JSON request");
        }

        @Test
        @DisplayName("emailul duplicat primeste 409 si nu creeaza al doilea rand")
        void duplicateEmail_returns409() {
            // Arrange
            register("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);

            // Act & Assert
            client.post().uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createBody("Altcineva", "Oarecare", "cristian.tudor@gmail.com", 44))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(409)
                    .jsonPath("$.message").isEqualTo("Email already used");

            assertThat(userRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("un id care nu e UUID primeste 400 cu numele parametrului")
        void nonUuidId_returns400() {
            // Act & Assert
            client.get().uri("/api/users/nu-e-uuid")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.message").value(message ->
                            assertThat((String) message).startsWith("Parameter 'id' has an invalid value"));
        }

        @Test
        @DisplayName("o ruta inexistenta primeste 404 cu ApiError, nu pagina de eroare a serverului")
        void unknownRoute_returns404WithApiError() {
            // Act & Assert
            client.get().uri("/api/nu-exista")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("No endpoint for this path")
                    .jsonPath("$.path").isEqualTo("/api/nu-exista");
        }

        @Test
        @DisplayName("PUT pe /{id} nu exista: modificarea se face doar cu PATCH")
        void putOnId_returns405() {
            // Arrange
            URI location = register("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);

            // Act & Assert
            client.put().uri(location)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"firstName\":\"Cristi\"}")
                    .exchange()
                    .expectStatus().isEqualTo(405);
        }
    }
}
