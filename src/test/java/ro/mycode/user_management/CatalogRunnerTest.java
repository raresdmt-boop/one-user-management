package ro.mycode.user_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import ro.mycode.user_management.users.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:demo_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@DisplayName("CatalogRunner (integrare) - catalogul de interogari pe profilul demo")
class CatalogRunnerTest {

    @Autowired
    private CatalogRunner catalogRunner;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("profilul demo aduce CatalogRunner in context")
    void demoProfile_registersCatalogRunner() {
        // Act & Assert
        assertThat(catalogRunner).isNotNull();
    }

    @Test
    @DisplayName("catalogul trece prin toate interogarile fara sa arunce")
    void runsEveryQueryWithoutThrowing() {
        // Arrange
        assertThat(userRepository.findByEmail("cristian.tudor@gmail.com")).isPresent();

        // Act & Assert
        assertThatCode(() -> catalogRunner.run()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ultima interogare din catalog schimba parola lui Cristian")
    void lastQueryChangesCristianPassword() {
        // Act
        catalogRunner.run();

        // Assert
        assertThat(userRepository.findByEmail("cristian.tudor@gmail.com").orElseThrow().getPassword())
                .isEqualTo("parolaNoua123");
    }
}
