package sisyphus_core.sisyphus_core.auth.exception;

public class DuplicateUserLoginIdException extends RuntimeException{
    public DuplicateUserLoginIdException(String message) {
        super(message);
    }
}
