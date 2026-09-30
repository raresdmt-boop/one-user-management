package ro.mycode.user_management.users.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ro.mycode.user_management.support.UserFixtures;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User (unitate) - constructor, egalitate, reprezentare")
class UserTest {

    @Test
    @DisplayName("constructorul public populeaza toate campurile si lasa id-ul null")
    void constructor_populatesFields_andLeavesIdNull() {
        // Arrange & Act
        User user = new User("Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", 30);

        // Assert
        assertThat(user.getId()).isNull();
        assertThat(user.getFirstName()).isEqualTo("Cristian");
        assertThat(user.getLastName()).isEqualTo("Tudor");
        assertThat(user.getEmail()).isEqualTo("cristian.tudor@gmail.com");
        assertThat(user.getPassword()).isEqualTo("parola123");
        assertThat(user.getAge()).isEqualTo(30);
    }

    @Test
    @DisplayName("setterii schimba doar campurile mutabile")
    void setters_changeMutableFields() {
        // Arrange
        User user = UserFixtures.user("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);

        // Act
        user.setFirstName("Radu");
        user.setLastName("Popescu");
        user.setEmail("radu.popescu@gmail.com");
        user.setPassword("altaparola1");
        user.setAge(41);

        // Assert
        assertThat(user.getFirstName()).isEqualTo("Radu");
        assertThat(user.getLastName()).isEqualTo("Popescu");
        assertThat(user.getEmail()).isEqualTo("radu.popescu@gmail.com");
        assertThat(user.getPassword()).isEqualTo("altaparola1");
        assertThat(user.getAge()).isEqualTo(41);
    }

    @Test
    @DisplayName("equals este true pentru aceeasi instanta")
    void equals_isTrue_forSameInstance() {
        // Arrange
        User user = UserFixtures.persisted();

        // Act & Assert
        assertThat(user.equals(user)).isTrue();
    }

    @Test
    @DisplayName("equals este false pentru doi useri fara id, chiar cu aceleasi date")
    void equals_isFalse_forTwoTransientUsersWithSameData() {
        // Arrange
        User first = UserFixtures.user("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);
        User second = UserFixtures.user("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);

        // Act & Assert
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("equals este true cand id-urile coincid, indiferent de restul datelor")
    void equals_isTrue_whenIdsMatch() {
        // Arrange
        User first = UserFixtures.persisted(UserFixtures.KNOWN_ID, "Cristian", "Tudor", "a@gmail.com", 30);
        User second = UserFixtures.persisted(UserFixtures.KNOWN_ID, "Radu", "Popescu", "b@gmail.com", 41);

        // Act & Assert
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("equals este false cand id-urile difera")
    void equals_isFalse_whenIdsDiffer() {
        // Arrange
        User first = UserFixtures.persisted(UserFixtures.KNOWN_ID, "Cristian", "Tudor", "a@gmail.com", 30);
        User second = UserFixtures.persisted(UserFixtures.OTHER_ID, "Cristian", "Tudor", "a@gmail.com", 30);

        // Act & Assert
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("equals este false pentru null si pentru alt tip")
    void equals_isFalse_forNullAndForeignType() {
        // Arrange
        User user = UserFixtures.persisted();

        // Act & Assert
        assertThat(user.equals(null)).isFalse();
        assertThat(user.equals("Cristian")).isFalse();
    }

    @Test
    @DisplayName("hashCode este identic pentru orice instanta de User")
    void hashCode_isStable_acrossInstances() {
        // Arrange
        User transientUser = UserFixtures.user("Cristian", "Tudor", "a@gmail.com", 30);
        User persistedUser = UserFixtures.persisted();

        // Act & Assert
        assertThat(transientUser.hashCode()).isEqualTo(persistedUser.hashCode());
    }

    @Test
    @DisplayName("toString nu expune parola")
    void toString_doesNotLeakPassword() {
        // Arrange
        User user = UserFixtures.persisted();

        // Act
        String text = user.toString();

        // Assert
        assertThat(text)
                .contains(UserFixtures.KNOWN_ID.toString())
                .contains("Cristian")
                .contains("Tudor")
                .contains("cristian.tudor@gmail.com")
                .contains("age=30")
                .doesNotContain(UserFixtures.VALID_PASSWORD);
    }
}
