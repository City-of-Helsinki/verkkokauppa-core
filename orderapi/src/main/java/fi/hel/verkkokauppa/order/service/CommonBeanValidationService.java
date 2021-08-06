package fi.hel.verkkokauppa.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

@Service
public class CommonBeanValidationService {

    @Autowired
    private Validator validator;

    public <T> void validateInput(T input){
        Set<ConstraintViolation<T>> set = validator.validate(input);

        if (!set.isEmpty()) {
            throw new ConstraintViolationException(set);
        }
    }

}
