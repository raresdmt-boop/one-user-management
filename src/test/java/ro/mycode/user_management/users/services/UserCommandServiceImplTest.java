package ro.mycode.user_management.users.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.mycode.user_management.support.UserFixtures;
import ro.mycode.user_management.users.dtos.ChangePasswordRequest;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.dtos.UserResponse;
import ro.mycode.user_management.users.dtos.UserUpdateRequest;
import ro.mycode.user_management.users.exceptions.EmailAlreadyUsed;
import ro.mycode.user_management.users.exceptions.ExceptionConstants;
import ro.mycode.user_management.users.exceptions.UserIdNotFound;
import ro.mycode.user_management.users.models.User;
import ro.mycode.user_management.users.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCommandServiceImpl (unitate, repository mock-uit)")
class UserCommandServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCommandServiceImpl service;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Nested
    @DisplayName("addUser")
    class AddUser {

        private final UserCreateRequest request = new UserCreateRequest(
                "Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", 30);

        @Test
        @DisplayName("salveaza si intoarce datele userului salvat, nu pe cele din cerere")
        void savesUser_andReturnsPersistedData() {
            // Arrange
            when(userRepository.existsByEmail("cristian.tudor@gmail.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(UserFixtures.persisted());

            // Act
            UserResponse response = service.addUser(request);

            // Assert
            assertThat(response.id()).isEqualTo(UserFixtures.KNOWN_ID);
            assertThat(response.firstName()).isEqualTo("Cristian");
            assertThat(response.lastName()).isEqualTo("Tudor");
            assertThat(response.email()).isEqualTo("cristian.tudor@gmail.com");
            assertThat(response.age()).isEqualTo(30);
        }

        @Test
        @DisplayName("construieste entitatea din toate cele cinci campuri ale cererii")
        void buildsEntityFromEveryRequestField() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(UserFixtures.persisted());

            // Act
            service.addUser(request);

            // Assert
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getFirstName()).isEqualTo("Cristian");
            assertThat(saved.getLastName()).isEqualTo("Tudor");
            assertThat(saved.getEmail()).isEqualTo("cristian.tudor@gmail.com");
            assertThat(saved.getPassword()).isEqualTo("parola123");
            assertThat(saved.getAge()).isEqualTo(30);
        }

        @Test
        @DisplayName("arunca EmailAlreadyUsed cand emailul exista si nu mai salveaza nimic")
        void throwsConflict_whenEmailAlreadyExists() {
            // Arrange
            when(userRepository.existsByEmail("cristian.tudor@gmail.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> service.addUser(request))
                    .isInstanceOf(EmailAlreadyUsed.class)
                    .hasMessage(ExceptionConstants.EMAIL_ALREADY_USED);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("arunca UserIdNotFound cand id-ul nu exista")
        void throwsNotFound_whenIdIsUnknown() {
            // Arrange
            UUID missingId = UserFixtures.OTHER_ID;
            when(userRepository.findById(missingId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.updateUser(missingId, new UserUpdateRequest("A", null, null, null)))
                    .isInstanceOf(UserIdNotFound.class)
                    .hasMessage(ExceptionConstants.USER_ID_NOT_FOUND);
        }

        @Test
        @DisplayName("actualizeaza toate cele patru campuri cand sunt trimise")
        void updatesEveryProvidedField() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));
            when(userRepository.existsByEmail("radu.popescu@gmail.com")).thenReturn(false);
            UserUpdateRequest request = new UserUpdateRequest("Radu", "Popescu", "radu.popescu@gmail.com", 41);

            // Act
            UserResponse response = service.updateUser(UserFixtures.KNOWN_ID, request);

            // Assert
            assertThat(existing.getFirstName()).isEqualTo("Radu");
            assertThat(existing.getLastName()).isEqualTo("Popescu");
            assertThat(existing.getEmail()).isEqualTo("radu.popescu@gmail.com");
            assertThat(existing.getAge()).isEqualTo(41);
            assertThat(response.firstName()).isEqualTo("Radu");
            assertThat(response.age()).isEqualTo(41);
        }

        @Test
        @DisplayName("o cerere cu toate campurile null nu schimba nimic")
        void allNullRequest_changesNothing() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));

            // Act
            UserResponse response = service.updateUser(
                    UserFixtures.KNOWN_ID, new UserUpdateRequest(null, null, null, null));

            // Assert
            assertThat(existing.getFirstName()).isEqualTo("Cristian");
            assertThat(existing.getLastName()).isEqualTo("Tudor");
            assertThat(existing.getEmail()).isEqualTo("cristian.tudor@gmail.com");
            assertThat(existing.getAge()).isEqualTo(30);
            assertThat(response.firstName()).isEqualTo("Cristian");
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("un sir format doar din spatii este ignorat, la fel ca null")
        void blankStrings_areIgnored() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));

            // Act
            service.updateUser(UserFixtures.KNOWN_ID, new UserUpdateRequest("  ", "   ", "   ", null));

            // Assert
            assertThat(existing.getFirstName()).isEqualTo("Cristian");
            assertThat(existing.getLastName()).isEqualTo("Tudor");
            assertThat(existing.getEmail()).isEqualTo("cristian.tudor@gmail.com");
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("acelasi email nu declanseaza verificarea de unicitate")
        void sameEmail_skipsUniquenessCheck() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));
            UserUpdateRequest request = new UserUpdateRequest(null, null, "cristian.tudor@gmail.com", null);

            // Act
            service.updateUser(UserFixtures.KNOWN_ID, request);

            // Assert
            assertThat(existing.getEmail()).isEqualTo("cristian.tudor@gmail.com");
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("un email nou, dar deja folosit de altcineva, arunca EmailAlreadyUsed")
        void newEmailTakenByAnother_throwsConflict() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));
            when(userRepository.existsByEmail("ocupat@gmail.com")).thenReturn(true);
            UserUpdateRequest request = new UserUpdateRequest(null, null, "ocupat@gmail.com", null);

            // Act & Assert
            assertThatThrownBy(() -> service.updateUser(UserFixtures.KNOWN_ID, request))
                    .isInstanceOf(EmailAlreadyUsed.class);
            assertThat(existing.getEmail()).isEqualTo("cristian.tudor@gmail.com");
        }

        @Test
        @DisplayName("varsta null lasa varsta veche neatinsa")
        void nullAge_leavesAgeUntouched() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));

            // Act
            service.updateUser(UserFixtures.KNOWN_ID, new UserUpdateRequest("Radu", null, null, null));

            // Assert
            assertThat(existing.getAge()).isEqualTo(30);
        }

        @Test
        @DisplayName("update-ul nu apeleaza save: se bazeaza pe dirty checking in tranzactie")
        void update_reliesOnDirtyChecking_andNeverCallsSave() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));

            // Act
            service.updateUser(UserFixtures.KNOWN_ID, new UserUpdateRequest("Radu", null, null, null));

            // Assert
            verify(userRepository, never()).save(any());
            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("sterge userul gasit si intoarce reprezentarea lui de dinainte de stergere")
        void deletesUser_andReturnsItsLastRepresentation() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));

            // Act
            UserResponse response = service.deleteUser(UserFixtures.KNOWN_ID);

            // Assert
            verify(userRepository).delete(existing);
            assertThat(response.id()).isEqualTo(UserFixtures.KNOWN_ID);
            assertThat(response.firstName()).isEqualTo("Cristian");
            assertThat(response.lastName()).isEqualTo("Tudor");
            assertThat(response.email()).isEqualTo("cristian.tudor@gmail.com");
            assertThat(response.age()).isEqualTo(30);
        }

        @Test
        @DisplayName("arunca UserIdNotFound si nu sterge nimic cand id-ul nu exista")
        void throwsNotFound_andDeletesNothing() {
            // Arrange
            when(userRepository.findById(UserFixtures.OTHER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.deleteUser(UserFixtures.OTHER_ID))
                    .isInstanceOf(UserIdNotFound.class);
            verify(userRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("schimba parola prin email-ul userului gasit si intoarce reprezentarea userului")
        void changesPasswordByTheFoundUserEmail_andReturnsTheUser() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));

            // Act
            UserResponse response = service.changePassword(
                    UserFixtures.KNOWN_ID, new ChangePasswordRequest("parolaNoua1"));

            // Assert
            verify(userRepository).updatePasswordByEmail("cristian.tudor@gmail.com", "parolaNoua1");
            assertThat(response.id()).isEqualTo(UserFixtures.KNOWN_ID);
            assertThat(response.email()).isEqualTo("cristian.tudor@gmail.com");
        }

        @Test
        @DisplayName("reprezentarea intoarsa nu poate conține parola: UserResponse nu are campul")
        void returnedRepresentation_cannotCarryThePassword() {
            // Arrange
            User existing = UserFixtures.persisted();
            when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(existing));

            // Act
            UserResponse response = service.changePassword(
                    UserFixtures.KNOWN_ID, new ChangePasswordRequest("parolaNoua1"));

            // Assert
            assertThat(response.toString()).doesNotContain("parolaNoua1");
        }

        @Test
        @DisplayName("arunca UserIdNotFound si nu atinge parola cand id-ul nu exista")
        void throwsNotFound_whenIdIsUnknown() {
            // Arrange
            when(userRepository.findById(UserFixtures.OTHER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.changePassword(
                    UserFixtures.OTHER_ID, new ChangePasswordRequest("parolaNoua1")))
                    .isInstanceOf(UserIdNotFound.class);
            verify(userRepository, never()).updatePasswordByEmail(anyString(), anyString());
        }
    }

    @Test
    @DisplayName("serviciul nu atinge repository-ul la construire")
    void constructor_doesNotTouchRepository() {
        // Arrange & Act
        new UserCommandServiceImpl(userRepository);

        // Assert
        verifyNoInteractions(userRepository);
    }
}
