package fi.hel.verkkokauppa.order.test.utils.annotations;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class RunIfProfileCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final var optional = findAnnotation(context.getElement(), RunIfProfile.class);
        if (optional.isPresent()) {
            final RunIfProfile annotation = optional.get();
            final String profile = annotation.profile();

            String activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");

            if (activeProfile != null && activeProfile.contains(profile)) {
                return ConditionEvaluationResult.enabled("Profile " + profile + " can run tests");
            } else {
                return ConditionEvaluationResult.disabled("Skipped test because profile : " + profile + " is not allowed to run it");
            }
        }
        return ConditionEvaluationResult.enabled("No assumptions, moving on...");
    }

}
