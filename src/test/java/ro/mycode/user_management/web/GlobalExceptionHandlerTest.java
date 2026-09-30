package ro.mycode.user_management.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.exceptions.EmailAlreadyUsed;
import ro.mycode.user_management.users.exceptions.EmailNotFound;
import ro.mycode.user_management.users.exceptions.UserIdNotFound;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProbeController.class)
@Import({ProbeController.class, ProbeService.class})
@DisplayName("GlobalExceptionHandler (integrare web) - traducerea excepțiilor in ApiError")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("UserIdNotFound devine 404")
    void userIdNotFound_becomes404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/user-id-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User ID not found"))
                .andExpect(jsonPath("$.path").value("/probe/user-id-not-found"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("EmailNotFound devine 404")
    void emailNotFound_becomes404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/email-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Email not found"));
    }

    @Test
    @DisplayName("EmailAlreadyUsed devine 409")
    void emailAlreadyUsed_becomes409() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/email-already-used"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already used"));
    }

    @Test
    @DisplayName("DataIntegrityViolationException devine 409 cu mesaj generic, nu cu textul din baza")
    void dataIntegrityViolation_becomes409WithGenericMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Request violates a database constraint"))
                .andExpect(jsonPath("$.message").value(not(containsString("uk_users_email"))));
    }

    @Test
    @DisplayName("o cale fara handler devine 404 cu mesaj de rută necunoscuta")
    void unknownPath_becomes404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/cale/care/nu/exista"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No endpoint for this path"));
    }

    @Test
    @DisplayName("timestamp-ul din ApiError este serializat, nu omis")
    void apiError_carriesTimestamp() throws Exception {
        // Act
        String body = mockMvc.perform(get("/probe/user-id-not-found"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(body).contains("\"timestamp\"");
    }

    @Test
    @DisplayName("o ConstraintViolationException cu cai simple devine 400 cu detalii sortate")
    void constraintViolation_withSimplePaths_becomes400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.length()").value(5))
                .andExpect(jsonPath("$.details[0]").value("age: Age must be greater than zero"))
                .andExpect(jsonPath("$.details[1]").value("email: Email must be a valid address"));
    }

    @Test
    @DisplayName("o validare de pe un bean @Validated urca cu cale cu puncte, din care se pastreaza doar campul")
    void constraintViolation_withNestedPaths_keepsOnlyFieldName() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/validated-service"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").value("age: Age must be greater than zero"))
                .andExpect(jsonPath("$.details[?(@ =~ /.*\\..*/)]").isEmpty());
    }

    @Test
    @DisplayName("pe un controller FARA @Validated, un parametru invalid da HandlerMethodValidationException")
    void handlerMethodValidation_firesWithoutValidatedOnTheController() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/positive").param("minAge", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").value("minAge: minAge must be greater than zero"));
    }

    @Test
    @DisplayName("doua incalcari pe parametri produc doua detalii, sortate alfabetic")
    void handlerMethodValidation_sortsDetails() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/probe/positive").param("minAge", "0").param("email", "nu-e-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[0]").value("email: Email must be a valid address"))
                .andExpect(jsonPath("$.details[1]").value("minAge: minAge must be greater than zero"));
    }

    @Test
    @DisplayName("un tip de excepție neinregistrat NU produce ApiError: nu exista handler pentru el")
    void unregisteredExceptionType_isNotTranslated() {
        // Act & Assert
        assertThatThrownBy(() -> mockMvc.perform(get("/probe/neinregistrat")))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("tip neinregistrat");
    }
}

@TestComponent
@RestController
@RequestMapping("/probe")
class ProbeController {

    private final ProbeService probeService;

    ProbeController(ProbeService probeService) {
        this.probeService = probeService;
    }

    @GetMapping("/user-id-not-found")
    void userIdNotFound() {
        throw new UserIdNotFound();
    }

    @GetMapping("/email-not-found")
    void emailNotFound() {
        throw new EmailNotFound();
    }

    @GetMapping("/email-already-used")
    void emailAlreadyUsed() {
        throw new EmailAlreadyUsed();
    }

    @GetMapping("/data-integrity")
    void dataIntegrity() {
        throw new DataIntegrityViolationException(
                "could not execute statement [Unique index or primary key violation: uk_users_email]");
    }

    @GetMapping("/constraint-violation")
    void constraintViolation() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<?>> violations = new HashSet<>(factory.getValidator()
                    .validate(new UserCreateRequest("", "", "nu-e-email", "scurt", 0)));
            throw new ConstraintViolationException(violations);
        }
    }

    @GetMapping("/validated-service")
    void validatedService() {
        probeService.accept(new UserCreateRequest("", "", "nu-e-email", "scurt", 0));
    }

    @GetMapping("/positive")
    void positive(@RequestParam @Positive(message = "minAge must be greater than zero") Integer minAge,
                  @RequestParam(required = false)
                  @Email(message = "Email must be a valid address") String email) {
    }

    @GetMapping("/neinregistrat")
    void unregistered() {
        throw new IllegalStateException("tip neinregistrat");
    }
}

@TestComponent
@Validated
class ProbeService {

    void accept(@Valid UserCreateRequest request) {
    }
}
