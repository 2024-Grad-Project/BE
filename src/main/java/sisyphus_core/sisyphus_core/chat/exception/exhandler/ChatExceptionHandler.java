package sisyphus_core.sisyphus_core.chat.exception.exhandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.exception.DuplicateChatRoomNameException;
import sisyphus_core.sisyphus_core.chat.exception.UserChatRoomNotFoundException;

@ControllerAdvice
public class ChatExceptionHandler {

    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<String> handleChatRoomNotFoundException(ChatRoomNotFoundException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(UserChatRoomNotFoundException.class)
    public ResponseEntity<String> handleUserChatRoomNotFoundException(UserChatRoomNotFoundException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateChatRoomNameException.class)
    public ResponseEntity<String> handleDuplicateChatRoomNameException(DuplicateChatRoomNameException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
