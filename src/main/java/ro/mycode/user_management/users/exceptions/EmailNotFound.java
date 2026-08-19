package ro.mycode.user_management.users.exceptions;

public class EmailNotFound extends RuntimeException {
    public EmailNotFound() {
        super(ExceptionConstants.EMAIL_NOT_FOUND);
    }
}
