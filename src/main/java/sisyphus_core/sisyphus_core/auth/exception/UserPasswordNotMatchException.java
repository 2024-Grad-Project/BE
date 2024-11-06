package sisyphus_core.sisyphus_core.auth.exception;

public class UserPasswordNotMatchException extends RuntimeException{
    public UserPasswordNotMatchException(String message) {
        super(message);
    }
}
