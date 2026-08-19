package ro.mycode.user_management.users.exceptions;

public class NoUsersFound extends RuntimeException {
    public NoUsersFound() {
        super(ExceptionConstants.NO_USERS_FOUND);
    }
}
