package ro.mycode.user_management.users.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record UserUpdateRequest(
        String firstName,

        String lastName,

        @Email(message = "Email must be a valid address")
        String email,

        @Positive(message = "Age must be greater than zero")
        @Max(value = 120, message = "Age must be at most 120")
        Integer age) {
}
