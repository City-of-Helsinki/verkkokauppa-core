package fi.hel.verkkokauppa.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonControllerAdvice {

    @ExceptionHandler(CommonApiException.class)
    public ResponseEntity<Errors> handleException(CommonApiException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getErrors());
    }

}
