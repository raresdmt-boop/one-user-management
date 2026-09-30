package ro.mycode.user_management.users.dtos;

public record EmailExistsResponse(String email, boolean exists) {
}
