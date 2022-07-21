package fi.hel.verkkokauppa.configuration.testing.annotations;



import org.junit.jupiter.api.extension.ExtendWith;

import fi.hel.verkkokauppa.configuration.testing.conditions.RunIfProfileCondition;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RunIfProfileCondition.class)
public @interface RunIfProfile {

    String profile();

}