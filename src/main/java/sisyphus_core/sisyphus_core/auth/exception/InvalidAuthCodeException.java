package sisyphus_core.sisyphus_core.auth.exception;

public class InvalidAuthCodeException extends RuntimeException{
    public InvalidAuthCodeException(String message) {
        super(message);
    }
}
