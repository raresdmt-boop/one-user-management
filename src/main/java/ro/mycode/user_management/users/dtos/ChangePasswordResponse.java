package ro.mycode.user_management.users.dtos;

import java.util.UUID;

public record ChangePasswordResponse(UUID id, String email, int updatedRows) {
}
