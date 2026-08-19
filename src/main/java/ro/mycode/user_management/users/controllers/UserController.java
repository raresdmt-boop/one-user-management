package ro.mycode.user_management.users.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import ro.mycode.user_management.users.dtos.ChangePasswordRequest;
import ro.mycode.user_management.users.dtos.ChangePasswordResponse;
import ro.mycode.user_management.users.dtos.PageResponse;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.dtos.UserCreateResponse;
import ro.mycode.user_management.users.dtos.UserDeleteResponse;
import ro.mycode.user_management.users.dtos.UserResponse;
import ro.mycode.user_management.users.dtos.UserSummary;
import ro.mycode.user_management.users.dtos.UserUpdateRequest;
import ro.mycode.user_management.users.dtos.UserUpdateResponse;
import ro.mycode.user_management.users.services.interfaces.UserCommandService;
import ro.mycode.user_management.users.services.interfaces.UserQueryService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserController(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    @PostMapping
    public ResponseEntity<UserCreateResponse> create(@Valid @RequestBody UserCreateRequest request,
                                                     UriComponentsBuilder uriBuilder) {

        UserCreateResponse created = userCommandService.addUser(request);

        URI location = uriBuilder.path("/api/users/{id}").buildAndExpand(created.id()).toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userQueryService.getUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userQueryService.getUserById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @Positive(message = "minAge must be greater than zero") Integer minAge,
            @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(userQueryService.search(name, minAge, pageable));
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getByEmail(
            @RequestParam @NotBlank(message = "email is required")
            @Email(message = "Email must be a valid address") String email) {

        return ResponseEntity.ok(userQueryService.getUserByEmail(email));
    }

    @GetMapping("/by-name")
    public ResponseEntity<List<UserResponse>> getByFullName(@RequestParam String firstName,
                                                            @RequestParam String lastName) {

        return ResponseEntity.ok(userQueryService.getUsersByFullName(firstName, lastName));
    }

    @GetMapping("/by-last-name")
    public ResponseEntity<List<UserResponse>> getByLastNameFragment(@RequestParam String contains) {
        return ResponseEntity.ok(userQueryService.getUsersByLastNameFragment(contains));
    }

    @GetMapping("/by-emails")
    public ResponseEntity<List<UserResponse>> getByEmails(@RequestParam List<String> emails) {
        return ResponseEntity.ok(userQueryService.getUsersByEmails(emails));
    }

    @GetMapping("/by-domain")
    public ResponseEntity<List<UserResponse>> getByEmailDomain(
            @RequestParam(defaultValue = "@gmail.com") String domain) {

        return ResponseEntity.ok(userQueryService.getUsersByEmailDomain(domain));
    }

    @GetMapping("/older-than")
    public ResponseEntity<List<UserResponse>> getOlderThan(@RequestParam int age) {
        return ResponseEntity.ok(userQueryService.getUsersOlderThan(age));
    }

    @GetMapping("/between-ages")
    public ResponseEntity<List<UserResponse>> getBetweenAges(@RequestParam int minAge,
                                                             @RequestParam int maxAge) {

        return ResponseEntity.ok(userQueryService.getUsersBetweenAges(minAge, maxAge));
    }

    @GetMapping("/adults")
    public ResponseEntity<List<UserResponse>> getAdults(@RequestParam(defaultValue = "18") int minAge) {
        return ResponseEntity.ok(userQueryService.getAdultUsers(minAge));
    }

    @GetMapping("/from-age")
    public ResponseEntity<PageResponse<UserResponse>> getFromAge(
            @RequestParam(defaultValue = "18") int age,
            @PageableDefault(size = 2, sort = "age", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(userQueryService.getUsersFromAge(age, pageable));
    }

    @GetMapping("/top3-by-age")
    public ResponseEntity<List<UserResponse>> getTop3ByAge() {
        return ResponseEntity.ok(userQueryService.getTop3ByAge());
    }

    @GetMapping("/summaries")
    public ResponseEntity<List<UserSummary>> getSummaries(@RequestParam String lastName) {
        return ResponseEntity.ok(userQueryService.getSummariesByLastName(lastName));
    }

    @GetMapping("/exists")
    public ResponseEntity<Map<String, Object>> emailExists(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("email", email, "exists", userQueryService.emailExists(email)));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> countYoungerThan(@RequestParam int youngerThan) {
        return ResponseEntity.ok(Map.of(
                "youngerThan", youngerThan,
                "count", userQueryService.countUsersYoungerThan(youngerThan)));
    }

    @GetMapping("/average-age")
    public ResponseEntity<Map<String, Object>> averageAge() {
        return ResponseEntity.ok(Map.of("averageAge", userQueryService.getAverageAge()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserUpdateResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UserUpdateRequest request) {

        return ResponseEntity.ok(userCommandService.updateUser(id, request));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ChangePasswordResponse> changePassword(@PathVariable UUID id,
                                                                 @Valid @RequestBody ChangePasswordRequest request) {

        return ResponseEntity.ok(userCommandService.changePassword(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserDeleteResponse> delete(@PathVariable UUID id) {
        return ResponseEntity.ok(userCommandService.deleteUser(id));
    }
}
