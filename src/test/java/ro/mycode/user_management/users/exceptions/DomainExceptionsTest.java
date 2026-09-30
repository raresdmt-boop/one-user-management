package ro.mycode.user_management.users.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Excepțiile de domeniu (unitate) - mesaj si ierarhie")
class DomainExceptionsTest {

    @Test
    @DisplayName("fiecare excepție isi ia mesajul din ExceptionConstants")
    void eachException_carriesItsConstantMessage() {
        // Arrange & Act & Assert
        assertThat(new UserIdNotFound()).hasMessage(ExceptionConstants.USER_ID_NOT_FOUND);
        assertThat(new EmailNotFound()).hasMessage(ExceptionConstants.EMAIL_NOT_FOUND);
        assertThat(new EmailAlreadyUsed()).hasMessage(ExceptionConstants.EMAIL_ALREADY_USED);
        assertThat(new NoUsersFound()).hasMessage(ExceptionConstants.NO_USERS_FOUND);
    }

    @Test
    @DisplayName("toate sunt RuntimeException, deci nu obliga la try-catch")
    void allExceptions_areUnchecked() {
        // Arrange & Act & Assert
        assertThat(new UserIdNotFound()).isInstanceOf(RuntimeException.class);
        assertThat(new EmailNotFound()).isInstanceOf(RuntimeException.class);
        assertThat(new EmailAlreadyUsed()).isInstanceOf(RuntimeException.class);
        assertThat(new NoUsersFound()).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("constantele au textul exact pe care il vede clientul API-ului")
    void constants_holdTheExactClientFacingText() {
        // Arrange & Act & Assert
        assertThat(ExceptionConstants.EMAIL_ALREADY_USED).isEqualTo("Email already used");
        assertThat(ExceptionConstants.EMAIL_NOT_FOUND).isEqualTo("Email not found");
        assertThat(ExceptionConstants.USER_ID_NOT_FOUND).isEqualTo("User ID not found");
        assertThat(ExceptionConstants.NO_USERS_FOUND).isEqualTo("No users found");
    }

    @Test
    @DisplayName("ExceptionConstants este doar un purtator de constante")
    void exceptionConstants_isOnlyAConstantHolder() {
        // Arrange & Act
        ExceptionConstants holder = new ExceptionConstants();

        // Assert
        assertThat(holder).isNotNull();
    }
}
