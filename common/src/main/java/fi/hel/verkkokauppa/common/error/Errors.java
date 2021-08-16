package fi.hel.verkkokauppa.common.error;

import java.util.ArrayList;
import java.util.List;

public class Errors {
    private List<Error> errors;

    public Errors() {
        errors = new ArrayList<Error>();
    }

    public Errors(String code) {
        super();
        add(code);
    }

    public Errors(String code, String message) {
        super();
        add(code, message);
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }
    
    public Errors add(Error error) {
        this.errors.add(error);
        return this;
    }

    public Errors add(String code, String message) {
        this.errors.add(new Error(code, message));
        return this;
    }

    public Errors add(String code) {
        this.errors.add(new Error(code, null));
        return this;
    }

}
