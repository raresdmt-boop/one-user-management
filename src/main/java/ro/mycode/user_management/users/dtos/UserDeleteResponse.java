package ro.mycode.user_management.users.dtos;

import java.util.UUID;

public record UserDeleteResponse(UUID id, String firstName, String lastName, String email) {
}
