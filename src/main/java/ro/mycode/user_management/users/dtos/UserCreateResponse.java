package ro.mycode.user_management.users.dtos;

import java.util.UUID;

public record UserCreateResponse(UUID id, String firstName, String lastName, String email, int age) {
}
