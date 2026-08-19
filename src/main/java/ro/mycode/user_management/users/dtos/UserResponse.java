package ro.mycode.user_management.users.dtos;

import ro.mycode.user_management.users.models.User;

import java.util.UUID;

public record UserResponse(UUID id, String firstName, String lastName, String email, int age) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAge());
    }
}
