package fi.hel.verkkokauppa.common.error;

import org.springframework.http.HttpStatus;

public class CommonApiException extends RuntimeException {

    private HttpStatus status;
    private Errors errors = new Errors();

    public CommonApiException(HttpStatus status, Errors errors) {
        this.status = status;
        this.errors = errors;
    }

    public CommonApiException(HttpStatus status, Error error) {
        this.status = status;
        errors.add(error);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Errors getErrors() {
        return errors;
    }
}
