package ro.mycode.user_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:context_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@DisplayName("UserManagementApplication (integrare) - contextul se ridica")
class UserManagementApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("contextul se incarca")
    void contextLoads() {
        // Act & Assert
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("aplicatia porneste si din metoda main")
    void mainStartsTheApplication() {
        // Act & Assert
        assertThatCode(() -> UserManagementApplication.main(new String[]{
                "--spring.datasource.url=jdbc:h2:mem:main_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "--server.port=0"
        })).doesNotThrowAnyException();
    }
}
