package ro.mycode.user_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.repository.UserRepository;
import ro.mycode.user_management.users.services.interfaces.UserCommandService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSeeder (unitate) - popularea initiala a bazei")
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCommandService userCommandService;

    @InjectMocks
    private DataSeeder seeder;

    @Test
    @DisplayName("pe baza goala insereaza cei cinci useri de seed")
    void emptyDatabase_seedsFiveUsers() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);

        // Act
        seeder.run();

        // Assert
        verify(userCommandService, times(5)).addUser(any(UserCreateRequest.class));
    }

    @Test
    @DisplayName("userii de seed au emailuri distincte si varste intre 17 si 41")
    void seededUsers_haveDistinctEmailsAndExpectedAges() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        ArgumentCaptor<UserCreateRequest> captor = ArgumentCaptor.forClass(UserCreateRequest.class);

        // Act
        seeder.run();

        // Assert
        verify(userCommandService, times(5)).addUser(captor.capture());
        List<UserCreateRequest> seeded = captor.getAllValues();
        assertThat(seeded).extracting(UserCreateRequest::email).doesNotHaveDuplicates();
        assertThat(seeded).extracting(UserCreateRequest::age).allSatisfy(age ->
                assertThat(age).isBetween(17, 41));
        assertThat(seeded).extracting(UserCreateRequest::password)
                .allSatisfy(password -> assertThat(password).hasSizeGreaterThanOrEqualTo(8));
    }

    @Test
    @DisplayName("pe o baza care are deja useri nu insereaza nimic")
    void nonEmptyDatabase_seedsNothing() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);

        // Act
        seeder.run();

        // Assert
        verify(userCommandService, never()).addUser(any());
    }

    @Test
    @DisplayName("un singur user existent este de ajuns ca seed-ul sa fie sarit")
    void oneExistingUser_isEnoughToSkipSeeding() {
        // Arrange
        when(userRepository.count()).thenReturn(1L);

        // Act
        seeder.run();

        // Assert
        verify(userCommandService, never()).addUser(any());
    }

    @Test
    @DisplayName("argumentele din linia de comanda sunt ignorate")
    void commandLineArguments_areIgnored() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);

        // Act
        seeder.run("--oarecare", "alt-argument");

        // Assert
        verify(userCommandService, times(5)).addUser(any());
    }
}
