package ro.mycode.user_management.users.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ro.mycode.user_management.users.dtos.PageResponse;
import ro.mycode.user_management.users.dtos.UserResponse;
import ro.mycode.user_management.users.dtos.UserSummary;
import ro.mycode.user_management.users.exceptions.EmailNotFound;
import ro.mycode.user_management.users.exceptions.NoUsersFound;
import ro.mycode.user_management.users.exceptions.UserIdNotFound;
import ro.mycode.user_management.users.models.User;
import ro.mycode.user_management.users.repository.UserRepository;
import ro.mycode.user_management.users.services.interfaces.UserQueryService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserResponse> getUsers() {
        return toResponses(userRepository.findAllByOrderByLastNameAscFirstNameAsc());
    }

    @Override
    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(UserIdNotFound::new);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(EmailNotFound::new);
    }

    @Override
    public PageResponse<UserResponse> search(String name, Integer minAge, Pageable pageable) {
        Page<User> page = userRepository.search(name, minAge, pageable);
        return PageResponse.from(page, UserResponse::from);
    }

    @Override
    public List<UserResponse> getUsersByFullName(String firstName, String lastName) {
        return toResponses(userRepository.findByFirstNameAndLastName(firstName, lastName));
    }

    @Override
    public List<UserResponse> getUsersByLastNameFragment(String fragment) {
        return toResponses(userRepository.findByLastNameContainingIgnoreCase(fragment));
    }

    @Override
    public List<UserResponse> getUsersByEmails(Collection<String> emails) {
        return toResponses(userRepository.findByEmailIn(emails));
    }

    @Override
    public List<UserResponse> getUsersByEmailDomain(String domain) {
        return toResponses(userRepository.findByEmailEndingWith(domain));
    }

    @Override
    public List<UserResponse> getUsersOlderThan(int age) {
        return toResponses(userRepository.findByAgeGreaterThan(age));
    }

    @Override
    public List<UserResponse> getUsersBetweenAges(int minAge, int maxAge) {
        return toResponses(userRepository.findByAgeBetween(minAge, maxAge));
    }

    @Override
    public List<UserResponse> getAdultUsers(int minAge) {
        return toResponses(userRepository.findAdultUsers(minAge));
    }

    @Override
    public PageResponse<UserResponse> getUsersFromAge(int age, Pageable pageable) {
        Page<User> page = userRepository.findByAgeGreaterThanEqual(age, pageable);
        return PageResponse.from(page, UserResponse::from);
    }

    @Override
    public List<UserResponse> getTop3ByAge() {
        return toResponses(userRepository.findTop3ByOrderByAgeDesc());
    }

    @Override
    public List<UserSummary> getSummariesByLastName(String lastName) {
        return userRepository.findByLastNameOrderByFirstNameAsc(lastName);
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public long countUsersYoungerThan(int age) {
        return userRepository.countByAgeLessThan(age);
    }

    @Override
    public Double getAverageAge() {
        Double average = userRepository.findAverageAge();
        if (average == null) {
            throw new NoUsersFound();
        }
        return average;
    }

    private List<UserResponse> toResponses(List<User> users) {
        return users.stream().map(UserResponse::from).toList();
    }
}
