package ro.mycode.user_management.users.services.interfaces;

import jakarta.validation.Valid;
import ro.mycode.user_management.users.dtos.ChangePasswordRequest;
import ro.mycode.user_management.users.dtos.ChangePasswordResponse;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.dtos.UserCreateResponse;
import ro.mycode.user_management.users.dtos.UserDeleteResponse;
import ro.mycode.user_management.users.dtos.UserUpdateRequest;
import ro.mycode.user_management.users.dtos.UserUpdateResponse;

import java.util.UUID;

public interface UserCommandService {

    UserCreateResponse addUser(@Valid UserCreateRequest userCreateRequest);

    UserUpdateResponse updateUser(UUID id, @Valid UserUpdateRequest userUpdateRequest);

    UserDeleteResponse deleteUser(UUID id);

    ChangePasswordResponse changePassword(UUID id, @Valid ChangePasswordRequest changePasswordRequest);
}
