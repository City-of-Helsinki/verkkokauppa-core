package fi.hel.verkkokauppa.common.sentry;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import io.sentry.Hint;
import org.springframework.stereotype.Component;
@Component
public class CustomBeforeSendCallback implements SentryOptions.BeforeSendCallback {
    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        if (event.getThrowable() instanceof CommonApiException) {
            if (!((CommonApiException) event.getThrowable()).getStatus().is5xxServerError()) {
                event.setLevel(SentryLevel.INFO);
            }
            ((CommonApiException) event.getThrowable()).getErrors().getErrors().forEach(error -> event.addBreadcrumb(String.format("code: %s\nmessage: %s", error.getCode(), error.getMessage())));
        }
        return event;
    }
}
