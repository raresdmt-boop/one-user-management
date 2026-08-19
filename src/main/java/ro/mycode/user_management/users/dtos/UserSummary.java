package ro.mycode.user_management.users.dtos;

import java.util.UUID;

public interface UserSummary {

    UUID getId();

    String getFirstName();

    String getEmail();
}
