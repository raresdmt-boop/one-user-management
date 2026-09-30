package ro.mycode.user_management.users.controllers;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.mycode.user_management.support.UserFixtures;
import ro.mycode.user_management.users.dtos.ChangePasswordRequest;
import ro.mycode.user_management.users.dtos.ChangePasswordResponse;
import ro.mycode.user_management.users.dtos.PageResponse;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.dtos.UserCreateResponse;
import ro.mycode.user_management.users.dtos.UserDeleteResponse;
import ro.mycode.user_management.users.dtos.UserResponse;
import ro.mycode.user_management.users.dtos.UserSummary;
import ro.mycode.user_management.users.dtos.UserUpdateRequest;
import ro.mycode.user_management.users.dtos.UserUpdateResponse;
import ro.mycode.user_management.users.exceptions.EmailAlreadyUsed;
import ro.mycode.user_management.users.exceptions.EmailNotFound;
import ro.mycode.user_management.users.exceptions.NoUsersFound;
import ro.mycode.user_management.users.exceptions.UserIdNotFound;
import ro.mycode.user_management.users.models.User;
import ro.mycode.user_management.users.services.interfaces.UserCommandService;
import ro.mycode.user_management.users.services.interfaces.UserQueryService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController (integrare web) - rute, coduri HTTP si JSON")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserCommandService userCommandService;

    @MockitoBean
    private UserQueryService userQueryService;

    private static final UUID ID = UserFixtures.KNOWN_ID;

    private static UserResponse cristian() {
        return new UserResponse(ID, "Cristian", "Tudor", "cristian.tudor@gmail.com", 30);
    }

    private static UserResponse radu() {
        return new UserResponse(UserFixtures.OTHER_ID, "Radu", "Popescu", "radu.popescu@gmail.com", 17);
    }

    @Nested
    @DisplayName("POST /api/users")
    class Create {

        private final UserCreateRequest request = new UserCreateRequest(
                "Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", 30);

        @Test
        @DisplayName("intoarce 201, corpul creat si header-ul Location")
        void validRequest_returns201WithLocation() throws Exception {
            // Arrange
            when(userCommandService.addUser(any()))
                    .thenReturn(new UserCreateResponse(ID, "Cristian", "Tudor", "cristian.tudor@gmail.com", 30));

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/users/" + ID))
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.firstName").value("Cristian"))
                    .andExpect(jsonPath("$.email").value("cristian.tudor@gmail.com"))
                    .andExpect(jsonPath("$.age").value(30));
        }

        @Test
        @DisplayName("trimite serviciului exact datele din corpul cererii")
        void forwardsRequestBodyToService() throws Exception {
            // Arrange
            when(userCommandService.addUser(any()))
                    .thenReturn(new UserCreateResponse(ID, "Cristian", "Tudor", "cristian.tudor@gmail.com", 30));
            ArgumentCaptor<UserCreateRequest> captor = ArgumentCaptor.forClass(UserCreateRequest.class);

            // Act
            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            verify(userCommandService).addUser(captor.capture());
            assertThat(captor.getValue().password()).isEqualTo("parola123");
        }

        @Test
        @DisplayName("un corp invalid intoarce 400 cu lista de detalii, fara sa atinga serviciul")
        void invalidBody_returns400WithDetails() throws Exception {
            // Arrange
            UserCreateRequest invalid = new UserCreateRequest("", "", "nu-e-email", "scurt", 0);

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.path").value("/api/users"))
                    .andExpect(jsonPath("$.details.length()").value(5))
                    .andExpect(jsonPath("$.details[0]").value("age: Age must be greater than zero"));
            verify(userCommandService, never()).addUser(any());
        }

        @Test
        @DisplayName("un email deja folosit intoarce 409")
        void duplicateEmail_returns409() throws Exception {
            // Arrange
            when(userCommandService.addUser(any())).thenThrow(new EmailAlreadyUsed());

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.message").value("Email already used"));
        }

        @Test
        @DisplayName("un JSON stricat intoarce 400 cu mesaj de corp ilizibil")
        void malformedJson_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"firstName\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request"));
        }

        @Test
        @DisplayName("un corp lipsa intoarce 400")
        void missingBody_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/users si /api/users/{id}")
    class Read {

        @Test
        @DisplayName("lista intoarce 200 si un tablou JSON")
        void getAll_returns200WithArray() throws Exception {
            // Arrange
            when(userQueryService.getUsers()).thenReturn(List.of(radu(), cristian()));

            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].lastName").value("Popescu"))
                    .andExpect(jsonPath("$[1].lastName").value("Tudor"));
        }

        @Test
        @DisplayName("lista goala intoarce 200 si un tablou gol")
        void getAll_emptyList_returns200() throws Exception {
            // Arrange
            when(userQueryService.getUsers()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("un id existent intoarce 200 si userul")
        void getById_returns200() throws Exception {
            // Arrange
            when(userQueryService.getUserById(ID)).thenReturn(cristian());

            // Act & Assert
            mockMvc.perform(get("/api/users/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()));
        }

        @Test
        @DisplayName("un id inexistent intoarce 404")
        void getById_unknownId_returns404() throws Exception {
            // Arrange
            when(userQueryService.getUserById(ID)).thenThrow(new UserIdNotFound());

            // Act & Assert
            mockMvc.perform(get("/api/users/{id}", ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("User ID not found"))
                    .andExpect(jsonPath("$.path").value("/api/users/" + ID));
        }

        @Test
        @DisplayName("un id care nu e UUID intoarce 400 cu mesaj de tip greșit")
        void getById_nonUuid_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/nu-e-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parameter 'id' has an invalid value: nu-e-uuid"));
            verify(userQueryService, never()).getUserById(any());
        }
    }

    @Nested
    @DisplayName("GET /api/users/search")
    class Search {

        @Test
        @DisplayName("intoarce pagina impachetata cu metadate")
        void search_returnsWrappedPage() throws Exception {
            // Arrange
            PageResponse<UserResponse> page = new PageResponse<>(List.of(cristian()), 0, 10, 1, 1, true);
            when(userQueryService.search(eq("tud"), eq(18), any(Pageable.class))).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/users/search").param("name", "tud").param("minAge", "18"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("fara parametri foloseste paginarea implicita: size 10, sortat pe lastName")
        void search_withoutParams_usesPageableDefaults() throws Exception {
            // Arrange
            when(userQueryService.search(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true));
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            // Act
            mockMvc.perform(get("/api/users/search")).andExpect(status().isOk());

            // Assert
            verify(userQueryService).search(eq(null), eq(null), captor.capture());
            Pageable pageable = captor.getValue();
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort().getOrderFor("lastName")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("lastName").getDirection())
                    .isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("page si size din query string ajung in Pageable")
        void search_honoursPageAndSizeParams() throws Exception {
            // Arrange
            when(userQueryService.search(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageResponse<>(List.of(), 2, 3, 0, 0, true));
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            // Act
            mockMvc.perform(get("/api/users/search").param("page", "2").param("size", "3"))
                    .andExpect(status().isOk());

            // Assert
            verify(userQueryService).search(any(), any(), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(captor.getValue().getPageSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("minAge zero sau negativ intoarce 400 pe Positive")
        void search_nonPositiveMinAge_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/search").param("minAge", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Validation failed"));
            verify(userQueryService, never()).search(any(), any(), any());
        }

        @Test
        @DisplayName("minAge nenumeric intoarce 400 pe conversie")
        void search_nonNumericMinAge_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/search").param("minAge", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parameter 'minAge' has an invalid value: abc"));
        }
    }

    @Nested
    @DisplayName("Rutele de citire cu parametri")
    class ParameterisedReads {

        @Test
        @DisplayName("by-email intoarce 200 pentru un email existent")
        void byEmail_returns200() throws Exception {
            // Arrange
            when(userQueryService.getUserByEmail("cristian.tudor@gmail.com")).thenReturn(cristian());

            // Act & Assert
            mockMvc.perform(get("/api/users/by-email").param("email", "cristian.tudor@gmail.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Cristian"));
        }

        @Test
        @DisplayName("by-email intoarce 404 pentru un email inexistent")
        void byEmail_unknown_returns404() throws Exception {
            // Arrange
            when(userQueryService.getUserByEmail(anyString())).thenThrow(new EmailNotFound());

            // Act & Assert
            mockMvc.perform(get("/api/users/by-email").param("email", "nu.exista@gmail.com"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Email not found"));
        }

        @Test
        @DisplayName("by-email cu email malformat intoarce 400")
        void byEmail_malformed_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/by-email").param("email", "nu-e-email"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));
            verify(userQueryService, never()).getUserByEmail(anyString());
        }

        @Test
        @DisplayName("by-email fara parametru intoarce 400 cu numele parametrului lipsa")
        void byEmail_missingParam_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/users/by-email"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Required query parameter 'email' is missing"));
        }

        @Test
        @DisplayName("by-name cere ambii parametri")
        void byName_returns200_andRequiresBothParams() throws Exception {
            // Arrange
            when(userQueryService.getUsersByFullName("Cristian", "Tudor")).thenReturn(List.of(cristian()));

            // Act & Assert
            mockMvc.perform(get("/api/users/by-name")
                            .param("firstName", "Cristian").param("lastName", "Tudor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            mockMvc.perform(get("/api/users/by-name").param("firstName", "Cristian"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Required query parameter 'lastName' is missing"));
        }

        @Test
        @DisplayName("by-last-name trimite fragmentul mai departe")
        void byLastName_returns200() throws Exception {
            // Arrange
            when(userQueryService.getUsersByLastNameFragment("ste")).thenReturn(List.of(cristian()));

            // Act & Assert
            mockMvc.perform(get("/api/users/by-last-name").param("contains", "ste"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("by-emails desparte lista dupa virgula")
        void byEmails_splitsCommaSeparatedList() throws Exception {
            // Arrange
            when(userQueryService.getUsersByEmails(List.of("a@gmail.com", "b@gmail.com")))
                    .thenReturn(List.of(cristian(), radu()));

            // Act & Assert
            mockMvc.perform(get("/api/users/by-emails").param("emails", "a@gmail.com,b@gmail.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("by-domain foloseste @gmail.com cand parametrul lipseste")
        void byDomain_usesDefaultValue() throws Exception {
            // Arrange
            when(userQueryService.getUsersByEmailDomain("@gmail.com")).thenReturn(List.of(cristian()));

            // Act & Assert
            mockMvc.perform(get("/api/users/by-domain"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
            verify(userQueryService).getUsersByEmailDomain("@gmail.com");
        }

        @Test
        @DisplayName("by-domain accepta un domeniu explicit")
        void byDomain_acceptsExplicitDomain() throws Exception {
            // Arrange
            when(userQueryService.getUsersByEmailDomain("@yahoo.com")).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/users/by-domain").param("domain", "@yahoo.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("older-than cere parametrul age")
        void olderThan_returns200_andRequiresAge() throws Exception {
            // Arrange
            when(userQueryService.getUsersOlderThan(25)).thenReturn(List.of(cristian()));

            // Act & Assert
            mockMvc.perform(get("/api/users/older-than").param("age", "25"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            mockMvc.perform(get("/api/users/older-than"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("between-ages trimite ambele limite")
        void betweenAges_forwardsBothBounds() throws Exception {
            // Arrange
            when(userQueryService.getUsersBetweenAges(15, 20)).thenReturn(List.of(radu()));

            // Act & Assert
            mockMvc.perform(get("/api/users/between-ages").param("minAge", "15").param("maxAge", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].age").value(17));
        }

        @Test
        @DisplayName("adults foloseste 18 ca varsta implicita")
        void adults_usesDefaultMinAge() throws Exception {
            // Arrange
            when(userQueryService.getAdultUsers(18)).thenReturn(List.of(cristian()));

            // Act & Assert
            mockMvc.perform(get("/api/users/adults")).andExpect(status().isOk());
            verify(userQueryService).getAdultUsers(18);
        }

        @Test
        @DisplayName("from-age foloseste paginarea implicita: size 2, sortat pe age descrescator")
        void fromAge_usesPageableDefaults() throws Exception {
            // Arrange
            when(userQueryService.getUsersFromAge(anyInt(), any(Pageable.class)))
                    .thenReturn(new PageResponse<>(List.of(cristian()), 0, 2, 1, 1, true));
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            // Act
            mockMvc.perform(get("/api/users/from-age"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(2));

            // Assert
            verify(userQueryService).getUsersFromAge(eq(18), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(2);
            assertThat(captor.getValue().getSort().getOrderFor("age").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("top3-by-age nu cere parametri")
        void top3_returns200() throws Exception {
            // Arrange
            when(userQueryService.getTop3ByAge()).thenReturn(List.of(cristian(), radu()));

            // Act & Assert
            mockMvc.perform(get("/api/users/top3-by-age"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("summaries serializeaza proiectia cu cele trei getters")
        void summaries_serialiseProjection() throws Exception {
            // Arrange
            when(userQueryService.getSummariesByLastName("Tudor")).thenReturn(List.of(summaryOf(cristian())));

            // Act & Assert
            mockMvc.perform(get("/api/users/summaries").param("lastName", "Tudor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(ID.toString()))
                    .andExpect(jsonPath("$[0].firstName").value("Cristian"))
                    .andExpect(jsonPath("$[0].email").value("cristian.tudor@gmail.com"));
        }

        @Test
        @DisplayName("exists intoarce emailul si raspunsul boolean")
        void exists_returnsEmailAndFlag() throws Exception {
            // Arrange
            when(userQueryService.emailExists("cristian.tudor@gmail.com")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(get("/api/users/exists").param("email", "cristian.tudor@gmail.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("cristian.tudor@gmail.com"))
                    .andExpect(jsonPath("$.exists").value(true));
        }

        @Test
        @DisplayName("count intoarce limita si numarul")
        void count_returnsThresholdAndCount() throws Exception {
            // Arrange
            when(userQueryService.countUsersYoungerThan(30)).thenReturn(3L);

            // Act & Assert
            mockMvc.perform(get("/api/users/count").param("youngerThan", "30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.youngerThan").value(30))
                    .andExpect(jsonPath("$.count").value(3));
        }

        @Test
        @DisplayName("average-age intoarce media")
        void averageAge_returnsAverage() throws Exception {
            // Arrange
            when(userQueryService.getAverageAge()).thenReturn(26.6);

            // Act & Assert
            mockMvc.perform(get("/api/users/average-age"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageAge").value(26.6));
        }

        @Test
        @DisplayName("average-age pe baza goala intoarce 404")
        void averageAge_noUsers_returns404() throws Exception {
            // Arrange
            when(userQueryService.getAverageAge()).thenThrow(new NoUsersFound());

            // Act & Assert
            mockMvc.perform(get("/api/users/average-age"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No users found"));
        }

        private UserSummary summaryOf(UserResponse response) {
            return new UserSummary() {
                @Override
                public UUID getId() {
                    return response.id();
                }

                @Override
                public String getFirstName() {
                    return response.firstName();
                }

                @Override
                public String getEmail() {
                    return response.email();
                }
            };
        }
    }

    @Nested
    @DisplayName("PUT si DELETE /api/users/{id}")
    class Write {

        @Test
        @DisplayName("update intoarce 200 si userul modificat")
        void update_returns200() throws Exception {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("Radu", null, null, 41);
            when(userCommandService.updateUser(eq(ID), any()))
                    .thenReturn(new UserUpdateResponse(ID, "Radu", "Tudor", "cristian.tudor@gmail.com", 41));

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Radu"))
                    .andExpect(jsonPath("$.age").value(41));
        }

        @Test
        @DisplayName("update pe un id inexistent intoarce 404")
        void update_unknownId_returns404() throws Exception {
            // Arrange
            when(userCommandService.updateUser(eq(ID), any())).thenThrow(new UserIdNotFound());

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("update cu email deja folosit intoarce 409")
        void update_duplicateEmail_returns409() throws Exception {
            // Arrange
            when(userCommandService.updateUser(eq(ID), any())).thenThrow(new EmailAlreadyUsed());

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ocupat@gmail.com\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("update cu varsta invalida intoarce 400 fara sa atinga serviciul")
        void update_invalidAge_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/users/{id}", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"age\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value("age: Age must be greater than zero"));
            verify(userCommandService, never()).updateUser(any(), any());
        }

        @Test
        @DisplayName("un corp gol este acceptat: update-ul este partial")
        void update_emptyBody_isAccepted() throws Exception {
            // Arrange
            when(userCommandService.updateUser(eq(ID), any()))
                    .thenReturn(new UserUpdateResponse(ID, "Cristian", "Tudor", "cristian.tudor@gmail.com", 30));

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("schimbarea parolei intoarce 200 si numarul de linii afectate")
        void changePassword_returns200() throws Exception {
            // Arrange
            when(userCommandService.changePassword(eq(ID), any()))
                    .thenReturn(new ChangePasswordResponse(ID, "cristian.tudor@gmail.com", 1));

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}/password", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ChangePasswordRequest("parolaNoua1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("cristian.tudor@gmail.com"))
                    .andExpect(jsonPath("$.updatedRows").value(1));
        }

        @Test
        @DisplayName("o parola prea scurta intoarce 400")
        void changePassword_shortPassword_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/users/{id}/password", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"scurt\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]")
                            .value("newPassword: Password must be at least 8 characters"));
            verify(userCommandService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("schimbarea parolei pe un id inexistent intoarce 404")
        void changePassword_unknownId_returns404() throws Exception {
            // Arrange
            when(userCommandService.changePassword(eq(ID), any())).thenThrow(new UserIdNotFound());

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}/password", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"parolaNoua1\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("delete intoarce 200 si datele userului sters")
        void delete_returns200WithDeletedUser() throws Exception {
            // Arrange
            when(userCommandService.deleteUser(ID))
                    .thenReturn(new UserDeleteResponse(ID, "Cristian", "Tudor", "cristian.tudor@gmail.com"));

            // Act & Assert
            mockMvc.perform(delete("/api/users/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.email").value("cristian.tudor@gmail.com"))
                    .andExpect(jsonPath("$.age").doesNotExist());
        }

        @Test
        @DisplayName("delete pe un id inexistent intoarce 404")
        void delete_unknownId_returns404() throws Exception {
            // Arrange
            when(userCommandService.deleteUser(ID)).thenThrow(new UserIdNotFound());

            // Act & Assert
            mockMvc.perform(delete("/api/users/{id}", ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User ID not found"));
        }
    }

    @Test
    @DisplayName("ApiError poarta mereu timestamp, status, error si path")
    void errorBody_alwaysCarriesTheFourCoreFields() throws Exception {
        // Arrange
        when(userQueryService.getUserById(ID)).thenThrow(new UserIdNotFound());

        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", ID))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/users/" + ID));
    }

    @Test
    @DisplayName("o entitate User nu ajunge niciodata direct in JSON-ul raspunsului")
    void responses_neverExposeTheEntity() throws Exception {
        // Arrange
        User entity = UserFixtures.persisted();
        when(userQueryService.getUsers()).thenReturn(List.of(UserResponse.from(entity)));

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }
}
