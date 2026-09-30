package ro.mycode.user_management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.models.User;
import ro.mycode.user_management.users.repository.UserRepository;
import ro.mycode.user_management.users.services.interfaces.UserCommandService;
import ro.mycode.user_management.users.services.interfaces.UserQueryService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API useri (integrare completa) - HTTP -> serviciu -> repository -> H2")
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private UserQueryService userQueryService;

    @BeforeEach
    void emptyDatabase() {
        // Arrange
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

    private UUID createCristian() {
        return userCommandService.addUser(new UserCreateRequest(
                "Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", 30)).id();
    }

    @Nested
    @DisplayName("Ciclul complet de viata al unui user")
    class Lifecycle {

        @Test
        @DisplayName("creare, citire, modificare si stergere prin HTTP, cu baza reala in spate")
        void fullCrudFlow() throws Exception {
            // Act & Assert
            String location = mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("Cristian", "Tudor", "cristian.tudor@gmail.com", 30)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andReturn().getResponse().getHeader("Location");

            assertThat(location).isNotNull();
            String id = location.substring(location.lastIndexOf('/') + 1);
            assertThat(userRepository.count()).isEqualTo(1);

            mockMvc.perform(get("/api/users/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("cristian.tudor@gmail.com"))
                    .andExpect(jsonPath("$.age").value(30));

            mockMvc.perform(patch("/api/users/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Cristi\",\"age\":31}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Cristi"))
                    .andExpect(jsonPath("$.age").value(31));

            User afterUpdate = userRepository.findById(UUID.fromString(id)).orElseThrow();
            assertThat(afterUpdate.getFirstName()).isEqualTo("Cristi");
            assertThat(afterUpdate.getAge()).isEqualTo(31);
            assertThat(afterUpdate.getLastName()).isEqualTo("Tudor");

            mockMvc.perform(put("/api/users/{id}/password", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"parolaNoua1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("cristian.tudor@gmail.com"))
                    .andExpect(jsonPath("$.password").doesNotExist());

            assertThat(userRepository.findById(UUID.fromString(id)).orElseThrow().getPassword())
                    .isEqualTo("parolaNoua1");

            mockMvc.perform(delete("/api/users/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("cristian.tudor@gmail.com"));

            assertThat(userRepository.count()).isZero();

            mockMvc.perform(get("/api/users/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("modificarea se salveaza fara apel explicit de save, prin dirty checking")
        void update_persistsWithoutExplicitSave() throws Exception {
            // Arrange
            UUID id = createCristian();

            // Act
            mockMvc.perform(patch("/api/users/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"cristi.tudor@gmail.com\"}"))
                    .andExpect(status().isOk());

            // Assert
            assertThat(userRepository.findByEmail("cristi.tudor@gmail.com")).isPresent();
            assertThat(userRepository.findByEmail("cristian.tudor@gmail.com")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Reguli de business verificate pe baza reala")
    class BusinessRules {

        @Test
        @DisplayName("al doilea user cu acelasi email primeste 409, iar baza rămâne cu un singur rand")
        void duplicateEmail_returns409_andLeavesOneRow() throws Exception {
            // Arrange
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("Cristian", "Tudor", "cristian.tudor@gmail.com", 30)))
                    .andExpect(status().isCreated());

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("Altcineva", "Oarecare", "cristian.tudor@gmail.com", 44)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already used"));

            assertThat(userRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("mutarea unui email pe alt user primeste 409")
        void updateToTakenEmail_returns409() throws Exception {
            // Arrange
            UUID cristianId = createCristian();
            userCommandService.addUser(new UserCreateRequest(
                    "Radu", "Popescu", "radu.popescu@gmail.com", "parola123", 17));

            // Act & Assert
            mockMvc.perform(patch("/api/users/{id}", cristianId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"radu.popescu@gmail.com\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("un id inexistent produce 404 pe toate rutele care il folosesc")
        void unknownId_returns404OnEveryRoute() throws Exception {
            // Arrange
            UUID missing = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(get("/api/users/{id}", missing)).andExpect(status().isNotFound());
            mockMvc.perform(patch("/api/users/{id}", missing)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"X\"}")).andExpect(status().isNotFound());
            mockMvc.perform(put("/api/users/{id}/password", missing)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newPassword\":\"parolaNoua1\"}")).andExpect(status().isNotFound());
            mockMvc.perform(delete("/api/users/{id}", missing)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("average-age intoarce 200 cu null pe baza goala si media pe baza populata")
        void averageAge_dependsOnData() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/average-age"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("{\"averageAge\":null}"));

            userCommandService.addUser(new UserCreateRequest("A", "A", "a@gmail.com", "parola123", 20));
            userCommandService.addUser(new UserCreateRequest("B", "B", "b@gmail.com", "parola123", 30));

            mockMvc.perform(get("/api/users/average-age"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageAge").value(25.0));
        }

        @Test
        @DisplayName("validarea de pe serviciu prinde o cerere invalida chiar fara stratul web")
        void serviceLayerValidation_rejectsInvalidRequest() {
            // Arrange
            UserCreateRequest invalid = new UserCreateRequest("", "", "nu-e-email", "scurt", 0);

            // Act & Assert
            assertThatThrownBy(() -> userCommandService.addUser(invalid))
                    .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
            assertThat(userRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Interogari de citire pe date reale")
    class Queries {

        @BeforeEach
        void seedFive() {
            // Arrange
            userCommandService.addUser(new UserCreateRequest(
                    "Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", 30));
            userCommandService.addUser(new UserCreateRequest(
                    "Bogdan", "Alexandrescu", "bogdan.alexandrescu@gmail.com", "parola456", 41));
            userCommandService.addUser(new UserCreateRequest(
                    "Ana", "Stefanescu", "ana.stefanescu@yahoo.com", "parola789", 19));
            userCommandService.addUser(new UserCreateRequest(
                    "Radu", "Popescu", "radu.popescu@gmail.com", "parola000", 17));
            userCommandService.addUser(new UserCreateRequest(
                    "Maria", "Stere", "maria.stere@gmail.com", "parola111", 26));
        }

        @Test
        @DisplayName("lista este sortata dupa nume, apoi prenume")
        void listIsSortedByLastNameThenFirstName() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(5))
                    .andExpect(jsonPath("$[0].lastName").value("Alexandrescu"))
                    .andExpect(jsonPath("$[4].lastName").value("Tudor"));
        }

        @Test
        @DisplayName("search filtreaza pe nume si varsta si pagineaza")
        void searchFiltersAndPaginates() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/search").param("name", "ste").param("minAge", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].lastName").value("Stere"));

            mockMvc.perform(get("/api/users/search").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.last").value(false));
        }

        @Test
        @DisplayName("by-email, by-name si by-last-name gasesc userii cautati")
        void lookupRoutesFindTheRightUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/by-email").param("email", "ana.stefanescu@yahoo.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Ana"));

            mockMvc.perform(get("/api/users/by-name")
                            .param("firstName", "Radu").param("lastName", "Popescu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            mockMvc.perform(get("/api/users/by-last-name").param("contains", "ste"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("rutele pe varsta respecta limitele")
        void ageRoutesRespectBoundaries() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/older-than").param("age", "30"))
                    .andExpect(jsonPath("$.length()").value(1));

            mockMvc.perform(get("/api/users/between-ages").param("minAge", "17").param("maxAge", "19"))
                    .andExpect(jsonPath("$.length()").value(2));

            mockMvc.perform(get("/api/users/adults"))
                    .andExpect(jsonPath("$.length()").value(4));

            mockMvc.perform(get("/api/users/top3-by-age"))
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].age").value(41));

            mockMvc.perform(get("/api/users/from-age"))
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(4));
        }

        @Test
        @DisplayName("rutele de domeniu, emailuri si proiectie intorc datele corecte")
        void domainEmailsAndProjectionRoutes() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/by-domain"))
                    .andExpect(jsonPath("$.length()").value(4));

            mockMvc.perform(get("/api/users/by-domain").param("domain", "@yahoo.com"))
                    .andExpect(jsonPath("$.length()").value(1));

            mockMvc.perform(get("/api/users/by-emails")
                            .param("emails", "cristian.tudor@gmail.com,nu.exista@gmail.com"))
                    .andExpect(jsonPath("$.length()").value(1));

            mockMvc.perform(get("/api/users/summaries").param("lastName", "Tudor"))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].firstName").value("Cristian"))
                    .andExpect(jsonPath("$[0].email").value("cristian.tudor@gmail.com"));
        }

        @Test
        @DisplayName("exists si count raspund pe datele reale")
        void existsAndCountAnswerOnRealData() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/exists").param("email", "maria.stere@gmail.com"))
                    .andExpect(jsonPath("$.exists").value(true));

            mockMvc.perform(get("/api/users/exists").param("email", "nu.exista@gmail.com"))
                    .andExpect(jsonPath("$.exists").value(false));

            mockMvc.perform(get("/api/users/count").param("youngerThan", "30"))
                    .andExpect(jsonPath("$.count").value(3));
        }

        @Test
        @DisplayName("serviciul de citire intoarce aceleasi date si apelat direct, fara HTTP")
        void queryService_returnsSameDataWithoutHttp() {
            // Act & Assert
            assertThat(userQueryService.getUsers()).hasSize(5);
            assertThat(userQueryService.getAverageAge()).isEqualTo((30 + 41 + 19 + 17 + 26) / 5.0);
            assertThat(userQueryService.emailExists("cristian.tudor@gmail.com")).isTrue();
            assertThat(userQueryService.countUsersYoungerThan(20)).isEqualTo(2);
            assertThat(userQueryService.getTop3ByAge()).hasSize(3);
        }
    }
}
