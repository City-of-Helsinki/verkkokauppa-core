package fi.hel.verkkokauppa.productmapping.testing.annotations;

import fi.hel.verkkokauppa.productmapping.testing.conditions.RunIfProfileCondition;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RunIfProfileCondition.class)
public @interface RunIfProfile {

    String profile();

}