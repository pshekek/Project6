package rita.exeptions;

import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerErrorException;

import javax.persistence.EntityNotFoundException;
import javax.validation.ValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<BaseResponse<Object>> authenticationException(AuthenticationException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(401, e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<BaseResponse<Object>> entityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(404, e.getMessage()));
    }

    @ExceptionHandler(ServerErrorException.class)
    public ResponseEntity<BaseResponse<Object>> internalServerException(ServerErrorException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(500, e.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<BaseResponse<Object>> validationException(ValidationException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<BaseResponse<Object>> entityAlreadyExistsException(EntityAlreadyExistsException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(409, e.getMessage()));
    }

}

