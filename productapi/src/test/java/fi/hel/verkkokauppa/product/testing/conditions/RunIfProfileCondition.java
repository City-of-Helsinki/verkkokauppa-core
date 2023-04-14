package fi.hel.verkkokauppa.product.testing.conditions;


import fi.hel.verkkokauppa.product.testing.annotations.RunIfProfile;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

public class RunIfProfileCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final var optional = AnnotationUtils.findAnnotation(context.getElement(), RunIfProfile.class);
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
