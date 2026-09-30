package ro.mycode.user_management.users.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ro.mycode.user_management.users.dtos.UserSummary;
import ro.mycode.user_management.users.models.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@DisplayName("UserRepository (integrare) - interogari pe H2 in memorie")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User cristian;
    private User bogdan;
    private User ana;
    private User radu;
    private User maria;

    @BeforeEach
    void seed() {
        // Arrange
        userRepository.deleteAll();
        cristian = persist("Cristian", "Tudor", "cristian.tudor@gmail.com", 30);
        bogdan = persist("Bogdan", "Alexandrescu", "bogdan.alexandrescu@gmail.com", 41);
        ana = persist("Ana", "Stefanescu", "ana.stefanescu@yahoo.com", 19);
        radu = persist("Radu", "Popescu", "radu.popescu@gmail.com", 17);
        maria = persist("Maria", "Stere", "maria.stere@gmail.com", 26);
        entityManager.flush();
        entityManager.clear();
    }

    private User persist(String firstName, String lastName, String email, int age) {
        return entityManager.persist(new User(firstName, lastName, email, "parola123", age));
    }

    @Nested
    @DisplayName("Operatii moștenite din JpaRepository")
    class Inherited {

        @Test
        @DisplayName("save genereaza un UUID pentru un user nou")
        void save_generatesUuid() {
            // Arrange
            User newUser = new User("Vlad", "Ionescu", "vlad.ionescu@gmail.com", "parola123", 22);

            // Act
            User saved = userRepository.saveAndFlush(newUser);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(userRepository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("findAll intoarce cei cinci useri din seed")
        void findAll_returnsSeededUsers() {
            // Act & Assert
            assertThat(userRepository.findAll()).hasSize(5);
        }

        @Test
        @DisplayName("count numara randurile din tabel")
        void count_countsRows() {
            // Act & Assert
            assertThat(userRepository.count()).isEqualTo(5);
        }

        @Test
        @DisplayName("findById pe un id inexistent intoarce Optional gol")
        void findById_unknownId_returnsEmpty() {
            // Act & Assert
            assertThat(userRepository.findById(UUID.randomUUID())).isEmpty();
        }

        @Test
        @DisplayName("delete scoate userul din tabel")
        void delete_removesRow() {
            // Act
            userRepository.delete(userRepository.findById(cristian.getId()).orElseThrow());
            userRepository.flush();

            // Assert
            assertThat(userRepository.findById(cristian.getId())).isEmpty();
            assertThat(userRepository.count()).isEqualTo(4);
        }

        @Test
        @DisplayName("constrangerea de unicitate pe email respinge un duplicat la flush")
        void duplicateEmail_violatesUniqueConstraint() {
            // Arrange
            User duplicate = new User("Altcineva", "Oarecare", "cristian.tudor@gmail.com", "parola123", 33);

            // Act & Assert
            assertThatThrownBy(() -> {
                userRepository.save(duplicate);
                userRepository.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Derived queries")
    class Derived {

        @Test
        @DisplayName("findByEmail gaseste userul dupa email exact")
        void findByEmail_findsExactMatch() {
            // Act
            Optional<User> found = userRepository.findByEmail("cristian.tudor@gmail.com");

            // Assert
            assertThat(found).isPresent();
            assertThat(found.orElseThrow().getFirstName()).isEqualTo("Cristian");
        }

        @Test
        @DisplayName("findByEmail este sensibil la litere mari si mici")
        void findByEmail_isCaseSensitive() {
            // Act & Assert
            assertThat(userRepository.findByEmail("CRISTIAN.TUDOR@GMAIL.COM")).isEmpty();
        }

        @Test
        @DisplayName("findByFirstNameAndLastName cere ambele criterii indeplinite")
        void findByFirstNameAndLastName_requiresBoth() {
            // Act & Assert
            assertThat(userRepository.findByFirstNameAndLastName("Cristian", "Tudor")).hasSize(1);
            assertThat(userRepository.findByFirstNameAndLastName("Cristian", "Popescu")).isEmpty();
        }

        @Test
        @DisplayName("findByAgeGreaterThan exclude valoarea de la limita")
        void findByAgeGreaterThan_excludesBoundary() {
            // Act
            List<User> older = userRepository.findByAgeGreaterThan(30);

            // Assert
            assertThat(older).extracting(User::getEmail).containsExactly("bogdan.alexandrescu@gmail.com");
        }

        @Test
        @DisplayName("findByAgeBetween include ambele limite")
        void findByAgeBetween_includesBothBoundaries() {
            // Act
            List<User> between = userRepository.findByAgeBetween(17, 19);

            // Assert
            assertThat(between).extracting(User::getAge).containsExactlyInAnyOrder(17, 19);
        }

        @Test
        @DisplayName("findByLastNameContainingIgnoreCase prinde fragmentul fara sa conteze litera")
        void findByLastNameContainingIgnoreCase_matchesFragment() {
            // Act
            List<User> found = userRepository.findByLastNameContainingIgnoreCase("ste");

            // Assert
            assertThat(found).extracting(User::getLastName)
                    .containsExactlyInAnyOrder("Stefanescu", "Stere");
        }

        @Test
        @DisplayName("existsByEmail nu aduce entitatea, doar raspunde da sau nu")
        void existsByEmail_answersWithoutLoadingEntity() {
            // Act & Assert
            assertThat(userRepository.existsByEmail("cristian.tudor@gmail.com")).isTrue();
            assertThat(userRepository.existsByEmail("nu.exista@gmail.com")).isFalse();
        }

        @Test
        @DisplayName("deleteByEmail sterge randul corespunzator")
        void deleteByEmail_removesMatchingRow() {
            // Act
            userRepository.deleteByEmail("radu.popescu@gmail.com");
            userRepository.flush();

            // Assert
            assertThat(userRepository.findByEmail("radu.popescu@gmail.com")).isEmpty();
            assertThat(userRepository.count()).isEqualTo(4);
        }

        @Test
        @DisplayName("deleteByEmail pe un email inexistent nu sterge nimic")
        void deleteByEmail_unknownEmail_deletesNothing() {
            // Act
            userRepository.deleteByEmail("nu.exista@gmail.com");
            userRepository.flush();

            // Assert
            assertThat(userRepository.count()).isEqualTo(5);
        }

        @Test
        @DisplayName("findTop3ByOrderByAgeDesc limiteaza la trei si sorteaza descrescator")
        void findTop3ByOrderByAgeDesc_limitsAndSorts() {
            // Act
            List<User> top = userRepository.findTop3ByOrderByAgeDesc();

            // Assert
            assertThat(top).extracting(User::getAge).containsExactly(41, 30, 26);
        }

        @Test
        @DisplayName("findByEmailIn ignora emailurile care nu exista")
        void findByEmailIn_ignoresMissingEmails() {
            // Act
            List<User> found = userRepository.findByEmailIn(
                    List.of("cristian.tudor@gmail.com", "nu.exista@gmail.com"));

            // Assert
            assertThat(found).hasSize(1);
        }

        @Test
        @DisplayName("findByEmailIn cu lista goala intoarce lista goala")
        void findByEmailIn_emptyInput_returnsEmpty() {
            // Act & Assert
            assertThat(userRepository.findByEmailIn(List.of())).isEmpty();
        }

        @Test
        @DisplayName("findByEmailEndingWith filtreaza pe domeniu")
        void findByEmailEndingWith_filtersByDomain() {
            // Act & Assert
            assertThat(userRepository.findByEmailEndingWith("@gmail.com")).hasSize(4);
            assertThat(userRepository.findByEmailEndingWith("@yahoo.com")).hasSize(1);
        }

        @Test
        @DisplayName("countByAgeLessThan exclude valoarea de la limita")
        void countByAgeLessThan_excludesBoundary() {
            // Act & Assert
            assertThat(userRepository.countByAgeLessThan(30)).isEqualTo(3);
            assertThat(userRepository.countByAgeLessThan(17)).isZero();
        }

        @Test
        @DisplayName("findAllByOrderByLastNameAscFirstNameAsc sorteaza dupa nume, apoi prenume")
        void findAllByOrderByLastNameAscFirstNameAsc_sortsByBothFields() {
            // Act
            List<User> ordered = userRepository.findAllByOrderByLastNameAscFirstNameAsc();

            // Assert
            assertThat(ordered).extracting(User::getLastName)
                    .containsExactly("Alexandrescu", "Popescu", "Stefanescu", "Stere", "Tudor");
        }

        @Test
        @DisplayName("findByAgeGreaterThanEqual include limita si respecta paginarea")
        void findByAgeGreaterThanEqual_paginates() {
            // Act
            Page<User> firstPage = userRepository.findByAgeGreaterThanEqual(
                    19, PageRequest.of(0, 2, Sort.by("age").descending()));

            // Assert
            assertThat(firstPage.getTotalElements()).isEqualTo(4);
            assertThat(firstPage.getTotalPages()).isEqualTo(2);
            assertThat(firstPage.getContent()).extracting(User::getAge).containsExactly(41, 30);
            assertThat(firstPage.isLast()).isFalse();
        }

        @Test
        @DisplayName("a doua pagina continua de unde s-a oprit prima")
        void findByAgeGreaterThanEqual_secondPage() {
            // Act
            Page<User> secondPage = userRepository.findByAgeGreaterThanEqual(
                    19, PageRequest.of(1, 2, Sort.by("age").descending()));

            // Assert
            assertThat(secondPage.getContent()).extracting(User::getAge).containsExactly(26, 19);
            assertThat(secondPage.isLast()).isTrue();
        }

        @Test
        @DisplayName("findByLastNameOrderByFirstNameAsc intoarce o proiectie, nu entitati")
        void findByLastNameOrderByFirstNameAsc_returnsProjection() {
            // Act
            List<UserSummary> summaries = userRepository.findByLastNameOrderByFirstNameAsc("Tudor");

            // Assert
            assertThat(summaries).hasSize(1);
            UserSummary summary = summaries.getFirst();
            assertThat(summary.getId()).isEqualTo(cristian.getId());
            assertThat(summary.getFirstName()).isEqualTo("Cristian");
            assertThat(summary.getEmail()).isEqualTo("cristian.tudor@gmail.com");
        }
    }

    @Nested
    @DisplayName("JPQL, nativ si agregare")
    class ExplicitQueries {

        @Test
        @DisplayName("findByEmailAndPassword cere ambele valori potrivite")
        void findByEmailAndPassword_requiresBothToMatch() {
            // Act & Assert
            assertThat(userRepository.findByEmailAndPassword("cristian.tudor@gmail.com", "parola123")).isPresent();
            assertThat(userRepository.findByEmailAndPassword("cristian.tudor@gmail.com", "greșit")).isEmpty();
        }

        @Test
        @DisplayName("findAdultUsers include exact valoarea minima")
        void findAdultUsers_includesMinAge() {
            // Act
            List<User> adults = userRepository.findAdultUsers(19);

            // Assert
            assertThat(adults).hasSize(4);
            assertThat(adults).extracting(User::getAge).contains(19);
        }

        @Test
        @DisplayName("findByEmailNative foloseste SQL brut si intoarce aceeasi entitate")
        void findByEmailNative_returnsSameEntity() {
            // Act
            Optional<User> found = userRepository.findByEmailNative("ana.stefanescu@yahoo.com");

            // Assert
            assertThat(found).isPresent();
            assertThat(found.orElseThrow().getId()).isEqualTo(ana.getId());
        }

        @Test
        @DisplayName("findAverageAge calculeaza media varstelor")
        void findAverageAge_computesAverage() {
            // Act
            Double average = userRepository.findAverageAge();

            // Assert
            assertThat(average).isEqualTo((30 + 41 + 19 + 17 + 26) / 5.0);
        }

        @Test
        @DisplayName("findAverageAge intoarce null pe tabel gol, nu zero")
        void findAverageAge_returnsNull_whenTableIsEmpty() {
            // Arrange
            userRepository.deleteAll();
            userRepository.flush();

            // Act & Assert
            assertThat(userRepository.findAverageAge()).isNull();
        }

        @Test
        @DisplayName("searchByName caută in prenume si in nume, fara sa conteze litera")
        void searchByName_looksInBothNames() {
            // Act & Assert
            assertThat(userRepository.searchByName("tud")).hasSize(1);
            assertThat(userRepository.searchByName("ANA")).hasSize(1);
            assertThat(userRepository.searchByName("ste")).hasSize(2);
        }

        @Test
        @DisplayName("searchByName fara potrivire intoarce lista goala")
        void searchByName_noMatch_returnsEmpty() {
            // Act & Assert
            assertThat(userRepository.searchByName("zzz")).isEmpty();
        }

        @Test
        @DisplayName("search filtreaza pe nume si varsta simultan")
        void search_filtersByNameAndAge() {
            // Act
            Page<User> page = userRepository.search("ste", 20, PageRequest.of(0, 10, Sort.by("lastName")));

            // Assert
            assertThat(page.getContent()).extracting(User::getLastName).containsExactly("Stere");
        }

        @Test
        @DisplayName("search cu ambele filtre null intoarce toti userii")
        void search_withNullFilters_returnsEveryone() {
            // Act
            Page<User> page = userRepository.search(null, null, PageRequest.of(0, 10, Sort.by("lastName")));

            // Assert
            assertThat(page.getTotalElements()).isEqualTo(5);
        }

        @Test
        @DisplayName("search cu doar minAge ignora filtrul de nume")
        void search_withOnlyMinAge_ignoresNameFilter() {
            // Act
            Page<User> page = userRepository.search(null, 30, PageRequest.of(0, 10, Sort.by("age")));

            // Assert
            assertThat(page.getContent()).extracting(User::getAge).containsExactly(30, 41);
        }

        @Test
        @DisplayName("search respecta dimensiunea paginii")
        void search_respectsPageSize() {
            // Act
            Page<User> page = userRepository.search(null, null, PageRequest.of(0, 2, Sort.by("lastName")));

            // Assert
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("updatePasswordByEmail schimba parola si raporteaza o linie")
        void updatePasswordByEmail_updatesRow() {
            // Act
            int updated = userRepository.updatePasswordByEmail("cristian.tudor@gmail.com", "parolaNoua1");

            // Assert
            assertThat(updated).isEqualTo(1);
            assertThat(userRepository.findByEmail("cristian.tudor@gmail.com").orElseThrow().getPassword())
                    .isEqualTo("parolaNoua1");
        }

        @Test
        @DisplayName("updatePasswordByEmail pe un email inexistent raporteaza zero linii")
        void updatePasswordByEmail_unknownEmail_reportsZero() {
            // Act & Assert
            assertThat(userRepository.updatePasswordByEmail("nu.exista@gmail.com", "parolaNoua1")).isZero();
        }

        @Test
        @DisplayName("clearAutomatically goleste contextul, deci citirea de dupa update vede valoarea noua")
        void updatePasswordByEmail_clearsPersistenceContext() {
            // Arrange
            User loaded = userRepository.findByEmail("maria.stere@gmail.com").orElseThrow();
            assertThat(loaded.getPassword()).isEqualTo("parola123");

            // Act
            userRepository.updatePasswordByEmail("maria.stere@gmail.com", "parolaNoua1");
            User reloaded = userRepository.findByEmail("maria.stere@gmail.com").orElseThrow();

            // Assert
            assertThat(reloaded.getPassword()).isEqualTo("parolaNoua1");
            assertThat(reloaded).isNotSameAs(loaded);
        }
    }

    @Test
    @DisplayName("seed-ul a produs cinci useri cu id-uri distincte")
    void seed_producedFiveDistinctUsers() {
        // Act & Assert
        assertThat(List.of(cristian, bogdan, ana, radu, maria))
                .extracting(User::getId)
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }
}
