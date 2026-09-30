package ro.mycode.user_management.users.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ro.mycode.user_management.support.UserFixtures;
import ro.mycode.user_management.users.models.User;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reguli de validare (unitate) - adnotarile Bean Validation")
class ValidationRulesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void openValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
    }

    @Nested
    @DisplayName("UserCreateRequest")
    class CreateRequest {

        @Test
        @DisplayName("o cerere completa si corecta nu produce nicio incalcare")
        void validRequest_hasNoViolations() {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    "Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", 30);

            // Act
            Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest(name = "firstName = \"{0}\" este respins")
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("firstName gol sau numai spatii cade pe NotBlank")
        void blankFirstName_isRejected(String firstName) {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    firstName, "Tudor", "cristian.tudor@gmail.com", "parola123", 30);

            // Act
            Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

            // Assert
            assertThat(messages(violations)).contains("First name is required");
        }

        @Test
        @DisplayName("firstName null cade pe NotBlank")
        void nullFirstName_isRejected() {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    null, "Tudor", "cristian.tudor@gmail.com", "parola123", 30);

            // Act & Assert
            assertThat(messages(validator.validate(request))).contains("First name is required");
        }

        @Test
        @DisplayName("lastName gol cade pe NotBlank")
        void blankLastName_isRejected() {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    "Cristian", "  ", "cristian.tudor@gmail.com", "parola123", 30);

            // Act & Assert
            assertThat(messages(validator.validate(request))).contains("Last name is required");
        }

        @ParameterizedTest(name = "email = \"{0}\" este respins")
        @ValueSource(strings = {"fara-la", "fara@", "@fara-local", "doua@@arobe.com"})
        @DisplayName("un email malformat cade pe Email")
        void malformedEmail_isRejected(String email) {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    "Cristian", "Tudor", email, "parola123", 30);

            // Act & Assert
            assertThat(messages(validator.validate(request))).contains("Email must be a valid address");
        }

        @Test
        @DisplayName("email gol cade pe NotBlank, nu pe Email")
        void blankEmail_isRejectedByNotBlank() {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    "Cristian", "Tudor", "", "parola123", 30);

            // Act & Assert
            assertThat(messages(validator.validate(request))).contains("Email is required");
        }

        @ParameterizedTest(name = "parola de {0} caractere este respinsa")
        @CsvSource({"1", "7"})
        @DisplayName("o parola mai scurta de 8 caractere cade pe Size")
        void shortPassword_isRejected(int length) {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    "Cristian", "Tudor", "cristian.tudor@gmail.com", "a".repeat(length), 30);

            // Act & Assert
            assertThat(messages(validator.validate(request)))
                    .contains("Password must be at least 8 characters");
        }

        @Test
        @DisplayName("o parola de exact 8 caractere trece")
        void passwordOfExactlyEightChars_isAccepted() {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    "Cristian", "Tudor", "cristian.tudor@gmail.com", "12345678", 30);

            // Act & Assert
            assertThat(validator.validate(request)).isEmpty();
        }

        @ParameterizedTest(name = "varsta {0} este respinsa")
        @CsvSource({"0", "-1", "121", "200"})
        @DisplayName("varsta in afara intervalului (0, 120] este respinsa")
        void ageOutOfRange_isRejected(int age) {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    "Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", age);

            // Act & Assert
            assertThat(validator.validate(request)).isNotEmpty();
        }

        @ParameterizedTest(name = "varsta {0} este acceptata")
        @CsvSource({"1", "120"})
        @DisplayName("marginile intervalului sunt acceptate")
        void ageAtBoundaries_isAccepted(int age) {
            // Arrange
            UserCreateRequest request = new UserCreateRequest(
                    "Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", age);

            // Act & Assert
            assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        @DisplayName("mai multe campuri invalide produc mai multe incalcari")
        void severalInvalidFields_produceSeveralViolations() {
            // Arrange
            UserCreateRequest request = new UserCreateRequest("", "", "nu-e-email", "scurt", 0);

            // Act
            Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).hasSize(5);
        }
    }

    @Nested
    @DisplayName("UserUpdateRequest")
    class UpdateRequest {

        @Test
        @DisplayName("o cerere complet goala este valida: update-ul este partial")
        void allNullRequest_isValid() {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest(null, null, null, null);

            // Act & Assert
            assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        @DisplayName("numele gol trece validarea, pentru ca nu are NotBlank")
        void blankNames_passValidation() {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("", "  ", null, null);

            // Act & Assert
            assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        @DisplayName("email malformat cade pe Email")
        void malformedEmail_isRejected() {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest(null, null, "nu-e-email", null);

            // Act & Assert
            assertThat(messages(validator.validate(request))).contains("Email must be a valid address");
        }

        @Test
        @DisplayName("varsta zero sau negativa cade pe Positive")
        void nonPositiveAge_isRejected() {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest(null, null, null, 0);

            // Act & Assert
            assertThat(messages(validator.validate(request))).contains("Age must be greater than zero");
        }

        @Test
        @DisplayName("varsta peste 120 cade pe Max")
        void ageAboveMax_isRejected() {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest(null, null, null, 121);

            // Act & Assert
            assertThat(messages(validator.validate(request))).contains("Age must be at most 120");
        }
    }

    @Nested
    @DisplayName("ChangePasswordRequest")
    class PasswordRequest {

        @Test
        @DisplayName("o parola de cel putin 8 caractere trece")
        void longEnoughPassword_isAccepted() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("parolaNoua1");

            // Act & Assert
            assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        @DisplayName("o parola scurta cade pe Size")
        void shortPassword_isRejected() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("scurt");

            // Act & Assert
            assertThat(messages(validator.validate(request)))
                    .contains("Password must be at least 8 characters");
        }

        @Test
        @DisplayName("o parola goala cade pe NotBlank")
        void blankPassword_isRejected() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("   ");

            // Act & Assert
            assertThat(messages(validator.validate(request))).contains("New password is required");
        }
    }

    @Nested
    @DisplayName("Entitatea User")
    class Entity {

        @Test
        @DisplayName("un user corect nu produce incalcari")
        void validUser_hasNoViolations() {
            // Arrange
            User user = UserFixtures.user("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);

            // Act & Assert
            assertThat(validator.validate(user)).isEmpty();
        }

        @Test
        @DisplayName("entitatea repeta regulile din DTO: email si varsta sunt validate si aici")
        void invalidUser_isRejectedAtEntityLevel() {
            // Arrange
            User user = UserFixtures.user("Cristian", "Tudor", "nu-e-email", 0);

            // Act
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Assert
            assertThat(messages(violations))
                    .contains("Email must be a valid address", "Age must be greater than zero");
        }
    }
}
