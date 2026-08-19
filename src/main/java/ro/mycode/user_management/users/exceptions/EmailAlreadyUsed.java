package ro.mycode.user_management.users.exceptions;

public class EmailAlreadyUsed extends RuntimeException {
    public EmailAlreadyUsed() {
        super(ExceptionConstants.EMAIL_ALREADY_USED);
    }
}
