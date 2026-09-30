package ro.mycode.user_management.users.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ro.mycode.user_management.support.UserFixtures;
import ro.mycode.user_management.users.models.User;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserResponse (unitate) - maparea entitate -> DTO")
class UserResponseTest {

    @Test
    @DisplayName("from copiaza cele cinci campuri publice")
    void from_copiesPublicFields() {
        // Arrange
        User user = UserFixtures.persisted();

        // Act
        UserResponse response = UserResponse.from(user);

        // Assert
        assertThat(response.id()).isEqualTo(UserFixtures.KNOWN_ID);
        assertThat(response.firstName()).isEqualTo("Cristian");
        assertThat(response.lastName()).isEqualTo("Tudor");
        assertThat(response.email()).isEqualTo("cristian.tudor@gmail.com");
        assertThat(response.age()).isEqualTo(30);
    }

    @Test
    @DisplayName("from nu are de unde sa scurga parola: DTO-ul nu are campul")
    void from_carriesNoPassword() {
        // Arrange
        User user = UserFixtures.persisted();

        // Act
        UserResponse response = UserResponse.from(user);

        // Assert
        assertThat(response.toString()).doesNotContain(UserFixtures.VALID_PASSWORD);
    }

    @Test
    @DisplayName("from accepta un user fara id")
    void from_acceptsTransientUser() {
        // Arrange
        User user = UserFixtures.user("Ana", "Stefanescu", "ana@yahoo.com", 19);

        // Act
        UserResponse response = UserResponse.from(user);

        // Assert
        assertThat(response.id()).isNull();
        assertThat(response.firstName()).isEqualTo("Ana");
    }
}
