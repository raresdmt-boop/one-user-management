package ro.mycode.user_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import ro.mycode.user_management.users.models.User;
import ro.mycode.user_management.users.repository.UserRepository;
import ro.mycode.user_management.users.services.interfaces.UserCommandService;
import ro.mycode.user_management.users.services.interfaces.UserQueryService;
import ro.mycode.user_management.web.GlobalExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:startup_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@DisplayName("Pornirea aplicatiei (integrare) - context, bean-uri si seed")
class ApplicationStartupIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("contextul porneste si are toate bean-urile stratului de useri")
    void contextStarts_withEveryUserLayerBean() {
        // Act & Assert
        assertThat(context.getBean(UserRepository.class)).isNotNull();
        assertThat(context.getBean(UserCommandService.class)).isNotNull();
        assertThat(context.getBean(UserQueryService.class)).isNotNull();
        assertThat(context.getBean(GlobalExceptionHandler.class)).isNotNull();
        assertThat(context.getBean(DataSeeder.class)).isNotNull();
    }

    @Test
    @DisplayName("CatalogRunner nu exista in contextul implicit: este legat de profilul demo")
    void catalogRunner_isAbsentWithoutDemoProfile() {
        // Act & Assert
        assertThat(context.getBeanNamesForType(CatalogRunner.class)).isEmpty();
    }

    @Test
    @DisplayName("DataSeeder ruleaza la pornirea contextului, deci baza vine deja populata")
    void dataSeeder_runsAtStartup() {
        // Act & Assert
        assertThat(userRepository.count()).isEqualTo(5);
        assertThat(userRepository.findByEmail("cristian.tudor@gmail.com")).isPresent();
        assertThat(userRepository.findAll()).extracting(User::getLastName)
                .containsExactlyInAnyOrder("Tudor", "Alexandrescu", "Stefanescu", "Popescu", "Stere");
    }

    @Test
    @DisplayName("de aici vine nevoia de izolare: orice test care numara randuri curata baza intai")
    void seededData_isWhyTestsMustCleanUp() {
        // Act & Assert
        assertThat(userRepository.count()).isPositive();
    }
}
