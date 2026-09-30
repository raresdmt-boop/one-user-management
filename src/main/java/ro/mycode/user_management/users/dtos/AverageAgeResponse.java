package ro.mycode.user_management.users.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record AverageAgeResponse(Double averageAge) {
}
