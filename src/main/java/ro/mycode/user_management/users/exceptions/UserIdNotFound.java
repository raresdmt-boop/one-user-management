package ro.mycode.user_management.users.exceptions;

public class UserIdNotFound extends RuntimeException {
    public UserIdNotFound() {
        super(ExceptionConstants.USER_ID_NOT_FOUND);
    }
}
