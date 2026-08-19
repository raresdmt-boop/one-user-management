package ro.mycode.user_management.users.services.interfaces;

import org.springframework.data.domain.Pageable;
import ro.mycode.user_management.users.dtos.PageResponse;
import ro.mycode.user_management.users.dtos.UserResponse;
import ro.mycode.user_management.users.dtos.UserSummary;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserQueryService {

    List<UserResponse> getUsers();

    UserResponse getUserById(UUID id);

    UserResponse getUserByEmail(String email);

    PageResponse<UserResponse> search(String name, Integer minAge, Pageable pageable);

    List<UserResponse> getUsersByFullName(String firstName, String lastName);

    List<UserResponse> getUsersByLastNameFragment(String fragment);

    List<UserResponse> getUsersByEmails(Collection<String> emails);

    List<UserResponse> getUsersByEmailDomain(String domain);

    List<UserResponse> getUsersOlderThan(int age);

    List<UserResponse> getUsersBetweenAges(int minAge, int maxAge);

    List<UserResponse> getAdultUsers(int minAge);

    PageResponse<UserResponse> getUsersFromAge(int age, Pageable pageable);

    List<UserResponse> getTop3ByAge();

    List<UserSummary> getSummariesByLastName(String lastName);

    boolean emailExists(String email);

    long countUsersYoungerThan(int age);

    Double getAverageAge();
}
