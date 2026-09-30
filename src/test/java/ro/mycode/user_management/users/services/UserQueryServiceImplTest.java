package ro.mycode.user_management.users.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ro.mycode.user_management.support.UserFixtures;
import ro.mycode.user_management.users.dtos.PageResponse;
import ro.mycode.user_management.users.dtos.UserResponse;
import ro.mycode.user_management.users.dtos.UserSummary;
import ro.mycode.user_management.users.exceptions.EmailNotFound;
import ro.mycode.user_management.users.exceptions.ExceptionConstants;
import ro.mycode.user_management.users.exceptions.NoUsersFound;
import ro.mycode.user_management.users.exceptions.UserIdNotFound;
import ro.mycode.user_management.users.models.User;
import ro.mycode.user_management.users.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryServiceImpl (unitate, repository mock-uit)")
class UserQueryServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryServiceImpl service;

    private static final User CRISTIAN =
            UserFixtures.persisted(UserFixtures.KNOWN_ID, "Cristian", "Tudor", "cristian.tudor@gmail.com", 30);
    private static final User RADU =
            UserFixtures.persisted(UserFixtures.OTHER_ID, "Radu", "Popescu", "radu.popescu@gmail.com", 17);

    @Test
    @DisplayName("getUsers deleaga la interogarea sortata si mapeaza fiecare user")
    void getUsers_delegatesToSortedQuery() {
        // Arrange
        when(userRepository.findAllByOrderByLastNameAscFirstNameAsc()).thenReturn(List.of(RADU, CRISTIAN));

        // Act
        List<UserResponse> responses = service.getUsers();

        // Assert
        assertThat(responses).extracting(UserResponse::lastName).containsExactly("Popescu", "Tudor");
        verify(userRepository).findAllByOrderByLastNameAscFirstNameAsc();
    }

    @Test
    @DisplayName("getUsers intoarce lista goala cand nu exista useri")
    void getUsers_returnsEmptyList_whenNoUsers() {
        // Arrange
        when(userRepository.findAllByOrderByLastNameAscFirstNameAsc()).thenReturn(List.of());

        // Act & Assert
        assertThat(service.getUsers()).isEmpty();
    }

    @Test
    @DisplayName("getUserById intoarce userul mapat cand exista")
    void getUserById_returnsMappedUser() {
        // Arrange
        when(userRepository.findById(UserFixtures.KNOWN_ID)).thenReturn(Optional.of(CRISTIAN));

        // Act
        UserResponse response = service.getUserById(UserFixtures.KNOWN_ID);

        // Assert
        assertThat(response.id()).isEqualTo(UserFixtures.KNOWN_ID);
        assertThat(response.email()).isEqualTo("cristian.tudor@gmail.com");
    }

    @Test
    @DisplayName("getUserById arunca UserIdNotFound pentru un id inexistent")
    void getUserById_throwsNotFound() {
        // Arrange
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getUserById(missing))
                .isInstanceOf(UserIdNotFound.class)
                .hasMessage(ExceptionConstants.USER_ID_NOT_FOUND);
    }

    @Test
    @DisplayName("getUserByEmail intoarce userul mapat cand exista")
    void getUserByEmail_returnsMappedUser() {
        // Arrange
        when(userRepository.findByEmail("cristian.tudor@gmail.com")).thenReturn(Optional.of(CRISTIAN));

        // Act & Assert
        assertThat(service.getUserByEmail("cristian.tudor@gmail.com").firstName()).isEqualTo("Cristian");
    }

    @Test
    @DisplayName("getUserByEmail arunca EmailNotFound pentru un email inexistent")
    void getUserByEmail_throwsEmailNotFound() {
        // Arrange
        when(userRepository.findByEmail("nu.exista@gmail.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getUserByEmail("nu.exista@gmail.com"))
                .isInstanceOf(EmailNotFound.class)
                .hasMessage(ExceptionConstants.EMAIL_NOT_FOUND);
    }

    @Test
    @DisplayName("search trimite filtrele mai departe si impacheteaza pagina primita")
    void search_forwardsFilters_andWrapsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2, Sort.by("lastName"));
        when(userRepository.search("tud", 18, pageable))
                .thenReturn(new PageImpl<>(List.of(CRISTIAN), pageable, 1));

        // Act
        PageResponse<UserResponse> response = service.search("tud", 18, pageable);

        // Assert
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.last()).isTrue();
        verify(userRepository).search("tud", 18, pageable);
    }

    @Test
    @DisplayName("search accepta filtre null, ceea ce inseamna fara filtru")
    void search_acceptsNullFilters() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.search(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(CRISTIAN, RADU), pageable, 2));

        // Act
        PageResponse<UserResponse> response = service.search(null, null, pageable);

        // Assert
        assertThat(response.content()).hasSize(2);
    }

    @Test
    @DisplayName("getUsersByFullName deleaga la derived query cu doua criterii")
    void getUsersByFullName_delegates() {
        // Arrange
        when(userRepository.findByFirstNameAndLastName("Cristian", "Tudor")).thenReturn(List.of(CRISTIAN));

        // Act & Assert
        assertThat(service.getUsersByFullName("Cristian", "Tudor")).hasSize(1);
    }

    @Test
    @DisplayName("getUsersByLastNameFragment deleaga la containing ignore case")
    void getUsersByLastNameFragment_delegates() {
        // Arrange
        when(userRepository.findByLastNameContainingIgnoreCase("ste")).thenReturn(List.of(CRISTIAN));

        // Act & Assert
        assertThat(service.getUsersByLastNameFragment("ste")).hasSize(1);
    }

    @Test
    @DisplayName("getUsersByEmails deleaga la findByEmailIn")
    void getUsersByEmails_delegates() {
        // Arrange
        List<String> emails = List.of("cristian.tudor@gmail.com", "nu.exista@gmail.com");
        when(userRepository.findByEmailIn(emails)).thenReturn(List.of(CRISTIAN));

        // Act & Assert
        assertThat(service.getUsersByEmails(emails)).hasSize(1);
    }

    @Test
    @DisplayName("getUsersByEmailDomain deleaga la findByEmailEndingWith")
    void getUsersByEmailDomain_delegates() {
        // Arrange
        when(userRepository.findByEmailEndingWith("@gmail.com")).thenReturn(List.of(CRISTIAN, RADU));

        // Act & Assert
        assertThat(service.getUsersByEmailDomain("@gmail.com")).hasSize(2);
    }

    @Test
    @DisplayName("getUsersOlderThan deleaga la findByAgeGreaterThan")
    void getUsersOlderThan_delegates() {
        // Arrange
        when(userRepository.findByAgeGreaterThan(25)).thenReturn(List.of(CRISTIAN));

        // Act & Assert
        assertThat(service.getUsersOlderThan(25)).extracting(UserResponse::age).containsExactly(30);
    }

    @Test
    @DisplayName("getUsersBetweenAges deleaga la findByAgeBetween")
    void getUsersBetweenAges_delegates() {
        // Arrange
        when(userRepository.findByAgeBetween(15, 20)).thenReturn(List.of(RADU));

        // Act & Assert
        assertThat(service.getUsersBetweenAges(15, 20)).extracting(UserResponse::age).containsExactly(17);
    }

    @Test
    @DisplayName("getAdultUsers deleaga la interogarea JPQL findAdultUsers")
    void getAdultUsers_delegates() {
        // Arrange
        when(userRepository.findAdultUsers(18)).thenReturn(List.of(CRISTIAN));

        // Act & Assert
        assertThat(service.getAdultUsers(18)).hasSize(1);
    }

    @Test
    @DisplayName("getUsersFromAge impacheteaza pagina primita de la repository")
    void getUsersFromAge_wrapsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2, Sort.by("age").descending());
        when(userRepository.findByAgeGreaterThanEqual(18, pageable))
                .thenReturn(new PageImpl<>(List.of(CRISTIAN), pageable, 3));

        // Act
        PageResponse<UserResponse> response = service.getUsersFromAge(18, pageable);

        // Assert
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.last()).isFalse();
    }

    @Test
    @DisplayName("getTop3ByAge deleaga la derived query cu Top3")
    void getTop3ByAge_delegates() {
        // Arrange
        when(userRepository.findTop3ByOrderByAgeDesc()).thenReturn(List.of(CRISTIAN, RADU));

        // Act & Assert
        assertThat(service.getTop3ByAge()).hasSize(2);
    }

    @Test
    @DisplayName("getSummariesByLastName intoarce proiectia asa cum vine, fara mapare")
    void getSummariesByLastName_returnsProjectionAsIs() {
        // Arrange
        UserSummary summary = new UserSummary() {
            @Override
            public UUID getId() {
                return UserFixtures.KNOWN_ID;
            }

            @Override
            public String getFirstName() {
                return "Cristian";
            }

            @Override
            public String getEmail() {
                return "cristian.tudor@gmail.com";
            }
        };
        when(userRepository.findByLastNameOrderByFirstNameAsc("Tudor")).thenReturn(List.of(summary));

        // Act
        List<UserSummary> summaries = service.getSummariesByLastName("Tudor");

        // Assert
        assertThat(summaries).containsExactly(summary);
        assertThat(summaries.getFirst().getFirstName()).isEqualTo("Cristian");
    }

    @Test
    @DisplayName("emailExists intoarce true si false dupa raspunsul repository-ului")
    void emailExists_mirrorsRepository() {
        // Arrange
        when(userRepository.existsByEmail("cristian.tudor@gmail.com")).thenReturn(true);
        when(userRepository.existsByEmail("nu.exista@gmail.com")).thenReturn(false);

        // Act & Assert
        assertThat(service.emailExists("cristian.tudor@gmail.com")).isTrue();
        assertThat(service.emailExists("nu.exista@gmail.com")).isFalse();
    }

    @Test
    @DisplayName("countUsersYoungerThan deleaga la countByAgeLessThan")
    void countUsersYoungerThan_delegates() {
        // Arrange
        when(userRepository.countByAgeLessThan(30)).thenReturn(2L);

        // Act & Assert
        assertThat(service.countUsersYoungerThan(30)).isEqualTo(2L);
    }

    @Test
    @DisplayName("getAverageAge intoarce media cand exista useri")
    void getAverageAge_returnsAverage() {
        // Arrange
        when(userRepository.findAverageAge()).thenReturn(23.5);

        // Act & Assert
        assertThat(service.getAverageAge()).isEqualTo(23.5);
    }

    @Test
    @DisplayName("getAverageAge arunca NoUsersFound cand agregarea intoarce null")
    void getAverageAge_throwsNoUsersFound_whenAggregateIsNull() {
        // Arrange
        when(userRepository.findAverageAge()).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> service.getAverageAge())
                .isInstanceOf(NoUsersFound.class)
                .hasMessage(ExceptionConstants.NO_USERS_FOUND);
    }
}
