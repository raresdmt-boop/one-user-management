package ro.mycode.user_management.users.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ro.mycode.user_management.users.dtos.ChangePasswordRequest;
import ro.mycode.user_management.users.dtos.ChangePasswordResponse;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.dtos.UserCreateResponse;
import ro.mycode.user_management.users.dtos.UserDeleteResponse;
import ro.mycode.user_management.users.dtos.UserUpdateRequest;
import ro.mycode.user_management.users.dtos.UserUpdateResponse;
import ro.mycode.user_management.users.exceptions.EmailAlreadyUsed;
import ro.mycode.user_management.users.exceptions.UserIdNotFound;
import ro.mycode.user_management.users.models.User;
import ro.mycode.user_management.users.repository.UserRepository;
import ro.mycode.user_management.users.services.interfaces.UserCommandService;

import java.util.UUID;

@Service
@Validated
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;

    public UserCommandServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserCreateResponse addUser(UserCreateRequest userCreateRequest) {

        if (userRepository.existsByEmail(userCreateRequest.email())) {
            throw new EmailAlreadyUsed();
        }

        User newUser = new User(
                userCreateRequest.firstName(),
                userCreateRequest.lastName(),
                userCreateRequest.email(),
                userCreateRequest.password(),
                userCreateRequest.age());

        User saved = userRepository.save(newUser);

        return new UserCreateResponse(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getAge());
    }

    @Override
    @Transactional
    public UserUpdateResponse updateUser(UUID id, UserUpdateRequest userUpdateRequest) {

        User user = userRepository.findById(id).orElseThrow(UserIdNotFound::new);

        if (userUpdateRequest.firstName() != null && !userUpdateRequest.firstName().isBlank()) {
            user.setFirstName(userUpdateRequest.firstName());
        }
        if (userUpdateRequest.lastName() != null && !userUpdateRequest.lastName().isBlank()) {
            user.setLastName(userUpdateRequest.lastName());
        }
        if (userUpdateRequest.email() != null && !userUpdateRequest.email().isBlank()) {
            if (!userUpdateRequest.email().equals(user.getEmail())
                    && userRepository.existsByEmail(userUpdateRequest.email())) {
                throw new EmailAlreadyUsed();
            }
            user.setEmail(userUpdateRequest.email());
        }
        if (userUpdateRequest.age() != null) {
            user.setAge(userUpdateRequest.age());
        }

        return new UserUpdateResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAge());
    }

    @Override
    @Transactional
    public UserDeleteResponse deleteUser(UUID id) {

        User user = userRepository.findById(id).orElseThrow(UserIdNotFound::new);

        userRepository.delete(user);

        return new UserDeleteResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(UUID id, ChangePasswordRequest changePasswordRequest) {

        User user = userRepository.findById(id).orElseThrow(UserIdNotFound::new);

        int updatedRows = userRepository.updatePasswordByEmail(user.getEmail(), changePasswordRequest.newPassword());

        return new ChangePasswordResponse(id, user.getEmail(), updatedRows);
    }
}
