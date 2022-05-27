package fi.hel.verkkokauppa.common.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class CommonControllerAdvice {

    @ExceptionHandler(CommonApiException.class)
    public ResponseEntity<Errors> handleException(CommonApiException e) {
        e.getErrors().getErrors().forEach(error -> log.debug("code: {} message: {}", error.getCode(), error.getMessage()));
        return ResponseEntity.status(e.getStatus()).body(e.getErrors());
    }


    /**
     * Method that check against {@code @Valid} Objects passed to controller endpoints
     *
     * @param exception
     * @return a {@code ErrorResponse}
     *
     */
    @ExceptionHandler(value= MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleException(MethodArgumentNotValidException exception) {

        List<ErrorModel> errorMessages = exception.getBindingResult().getFieldErrors().stream()
                .map(err -> new ErrorModel(err.getField(), err.getRejectedValue(), err.getDefaultMessage()))
                .distinct()
                .collect(Collectors.toList());
        errorMessages.forEach(errorModel -> log.debug(errorModel.getMessageError()));
        return ErrorResponse.builder().errorMessage(errorMessages).build();
    }

}
