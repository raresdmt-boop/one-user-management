package ro.mycode.user_management.support;

import org.springframework.test.util.ReflectionTestUtils;
import ro.mycode.user_management.users.models.User;

import java.util.UUID;

public final class UserFixtures {

    public static final UUID KNOWN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final String VALID_PASSWORD = "parola123";

    private UserFixtures() {
    }

    public static User user(String firstName, String lastName, String email, int age) {
        return new User(firstName, lastName, email, VALID_PASSWORD, age);
    }

    private static User withId(UUID id, User user) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static User persisted() {
        return withId(KNOWN_ID, user("Cristian", "Tudor", "cristian.tudor@gmail.com", 30));
    }

    public static User persisted(UUID id, String firstName, String lastName, String email, int age) {
        return withId(id, user(firstName, lastName, email, age));
    }
}
