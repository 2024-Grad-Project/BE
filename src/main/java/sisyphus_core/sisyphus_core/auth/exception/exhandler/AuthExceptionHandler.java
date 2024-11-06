package sisyphus_core.sisyphus_core.auth.exception.exhandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import sisyphus_core.sisyphus_core.auth.exception.*;

@ControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<String> handleDuplicateUserException(DuplicateUserException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserLoginIdException.class)
    public ResponseEntity<String> handleDuplicateUserLoginIdException(DuplicateUserLoginIdException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserNicknameException.class)
    public ResponseEntity<String> handleDuplicateUserNicknameException(DuplicateUserNicknameException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidAuthCodeException.class)
    public ResponseEntity<String> handleInvalidAuthCodeException(InvalidAuthCodeException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(UserPasswordNotMatchException.class)
    public ResponseEntity<String> handleUserPasswordNotMatchException(UserPasswordNotMatchException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
