package ro.mycode.user_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ro.mycode.user_management.config.OpenApiConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:openapi_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@DisplayName("Documentația OpenAPI (integrare) - completa si sincronizata cu codul")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenApiConfig openApiConfig;

    @Test
    @DisplayName("bean-ul OpenAPI poarta titlul, versiunea si convenția de statusuri")
    void openApiBean_carriesTitleVersionAndStatusConvention() {
        // Act & Assert
        var api = openApiConfig.userManagementOpenApi();
        assertThat(api.getInfo().getTitle()).isEqualTo("User Management API");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(api.getInfo().getDescription())
                .contains("201 Created")
                .contains("409 Conflict")
                .contains("averageAge");
        assertThat(api.getServers()).hasSize(1);
    }

    @Test
    @DisplayName("/v3/api-docs se serveste si descrie toate cele 21 de rute")
    void apiDocs_describesEveryRoute() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("User Management API"))
                .andExpect(jsonPath("$.paths['/api/users'].post").exists())
                .andExpect(jsonPath("$.paths['/api/users'].get").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}/password'].put").exists())
                .andExpect(jsonPath("$.paths.length()").value(18));
    }

    @Test
    @DisplayName("controllerul-sonda din teste NU ajunge in documentatie si nici in aplicatie")
    void apiDocs_doesNotLeakTheTestProbeController() throws Exception {
        // Act
        String docs = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(docs).doesNotContain("/probe");
        mockMvc.perform(get("/probe/user-id-not-found")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT pe /api/users/{id} NU este documentat: update-ul se face cu PATCH")
    void apiDocs_doesNotDocumentPutOnUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/users/{id}'].put").doesNotExist());
    }

    @Test
    @DisplayName("crearea este singura ruta documentata cu 201")
    void apiDocs_onlyCreateDocuments201() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/users'].post.responses.201").exists())
                .andExpect(jsonPath("$.paths['/api/users'].post.responses.409").exists())
                .andExpect(jsonPath("$.paths['/api/users'].get.responses.201").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/users/{id}'].delete.responses.201").doesNotExist());
    }

    @Test
    @DisplayName("nicio ruta nu documenteaza 204: toate raspunsurile au corp")
    void apiDocs_documentsNo204() throws Exception {
        // Act
        String docs = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(docs).doesNotContain("\"204\"");
    }

    @Test
    @DisplayName("schema ApiError este inregistrata, cu toate cele sase campuri")
    void apiDocs_registersApiErrorSchema() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.ApiError").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.timestamp").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.status").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.error").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.message").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.path").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.details").exists());
    }

    @ParameterizedTest(name = "schema {0} este documentata")
    @ValueSource(strings = {"UserResponse", "UserCreateRequest", "UserUpdateRequest",
            "ChangePasswordRequest", "EmailExistsResponse", "UserCountResponse",
            "AverageAgeResponse", "UserSummary"})
    @DisplayName("fiecare DTO expus ajunge in components.schemas")
    void apiDocs_registersEveryExposedDto(String schemaName) throws Exception {
        // Act & Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas." + schemaName).exists());
    }

    @Test
    @DisplayName("parola nu apare in nicio schema de raspuns")
    void apiDocs_noResponseSchemaExposesThePassword() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.UserResponse.properties.password").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UserSummary.properties.password").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UserCreateRequest.properties.password").exists());
    }

    @Test
    @DisplayName("parametrii de paginare sunt documentati pe rutele paginate")
    void apiDocs_documentsPageableParameters() throws Exception {
        // Act
        String docs = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(docs).contains("\"/api/users/search\"").contains("\"/api/users/from-age\"");
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/users/search'].get.parameters[?(@.name == 'page')]")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/users/search'].get.parameters[?(@.name == 'size')]")
                        .exists());
    }

    @Test
    @DisplayName("fiecare operație are summary, ca lista din Swagger UI sa fie citibila")
    void apiDocs_everyOperationHasASummary() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$..operationId").exists())
                .andExpect(jsonPath("$.paths['/api/users'].post.summary")
                        .value("Creeaza un utilizator"))
                .andExpect(jsonPath("$.paths['/api/users/exists'].get.summary")
                        .value("Daca un email este deja folosit"));
    }

    @Test
    @DisplayName("Swagger UI raspunde si duce la pagina de documentatie")
    void swaggerUi_isServed() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
