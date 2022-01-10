package fi.hel.verkkokauppa.payment.logic.validation;

import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import org.helsinki.vismapay.util.ReturnDataAuthCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class PaymentReturnValidator {
    
    private Logger log = LoggerFactory.getLogger(PaymentReturnValidator.class);

    @Autowired
    private Environment env;


    public boolean validateChecksum(String authCode, String returnCode, String orderNumber, String settled, String incidentId) {
        if (buildAuthCodeFromReturnData(returnCode, orderNumber, settled, incidentId).equals(authCode)) {
			return true;
		} else {
            log.debug("payment validation failed, orderNumber: " + orderNumber);
			return false;
		}
	}

	public PaymentReturnDto validateReturnValues(boolean isValid, String returnCode, String settled) {
		boolean isPaymentPaid = false;
		boolean canRetry = false;
		boolean isAuthorized = false;

		if (isSuccesfull(returnCode) && isSettled(settled)) {
			isPaymentPaid = true;
		} else if (isSuccesfull(returnCode) && isAuthorized(settled)){
			isAuthorized = true;
		} else {
			// returnCode 4 = "Transaction status could not be updated after customer returned from a payment facilitator's web page. Please use the merchant UI to resolve the payment status."
			if (!"4".equals(returnCode)) {
				canRetry = true;
			}
		}

		return new PaymentReturnDto(isValid, isPaymentPaid, canRetry, isAuthorized);
	}

	private boolean isSettled(String settled) {
		return "1".equals(settled);
	}

	private boolean isAuthorized(String settled) {
		return "0".equals(settled);
	}

	private boolean isSuccesfull(String returnCode) {
		return "0".equals(returnCode);
	}

	private String buildAuthCodeFromReturnData(String returnCode, String orderNumber, String settled, String incidentId) {
        String encryptionKey = env.getRequiredProperty("payment_encryption_key");

		ReturnDataAuthCodeBuilder builder = new ReturnDataAuthCodeBuilder(
                encryptionKey,
				Short.valueOf(returnCode),
				orderNumber
		);

		if (settled != null && !settled.isEmpty()) {
			builder.withSettled(Byte.valueOf(settled));
		}
		if (incidentId != null && !incidentId.isEmpty()) {
			builder.withIncidentId(incidentId);
		}

		String calculatedAuthcode = builder.build();
        log.debug("calculatedAuthcode: " + calculatedAuthcode);

        return calculatedAuthcode;
	}
    
}
