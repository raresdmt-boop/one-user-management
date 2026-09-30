package ro.mycode.user_management.users.services.interfaces;

import jakarta.validation.Valid;
import ro.mycode.user_management.users.dtos.ChangePasswordRequest;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.dtos.UserResponse;
import ro.mycode.user_management.users.dtos.UserUpdateRequest;

import java.util.UUID;

public interface UserCommandService {

    UserResponse addUser(@Valid UserCreateRequest userCreateRequest);

    UserResponse updateUser(UUID id, @Valid UserUpdateRequest userUpdateRequest);

    UserResponse deleteUser(UUID id);

    UserResponse changePassword(UUID id, @Valid ChangePasswordRequest changePasswordRequest);
}
