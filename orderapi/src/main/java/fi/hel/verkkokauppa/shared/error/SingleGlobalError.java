package fi.hel.verkkokauppa.shared.error;

import org.springframework.validation.AbstractErrors;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.Collections;
import java.util.List;

public class SingleGlobalError extends AbstractErrors {

	private static final long serialVersionUID = 8822153417610110065L;

	private final String objectName;
	private final List<ObjectError> singleErrorList;

	public SingleGlobalError(String objectName, String code) {
		this(objectName, new String[] {code}, null, null);
	}

	public SingleGlobalError(String objectName, String code, Object[] arguments, String defaultMessage) {
		this(objectName, new String[] {code}, arguments, defaultMessage);
	}

	public SingleGlobalError(String objectName, String[] codes, Object[] arguments, String defaultMessage) {
		if (objectName == null || objectName.isEmpty()) {
			throw new IllegalArgumentException("Value of argument 'objectName' may not be null nor empty.");
		}
		if (codes == null || codes.length == 0) {
			throw new IllegalArgumentException("Value of argument 'codes' may not be null nor empty.");
		}

		this.objectName = objectName;
		this.singleErrorList = Collections.singletonList(new ObjectError(objectName, codes, arguments, defaultMessage));
	}

	@Override
	public String getObjectName() {
		return objectName;
	}

	@Override
	public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
		throw new UnsupportedOperationException("Adding more errors to a [" + SingleGlobalError.class.getSimpleName() + "] is not supported.");
	}

	@Override
	public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
		throw new UnsupportedOperationException("Adding more errors to a [" + SingleGlobalError.class.getSimpleName() + "] is not supported.");
	}

	@Override
	public void addAllErrors(Errors errors) {
		throw new UnsupportedOperationException("Adding more errors to " + SingleGlobalError.class.getSimpleName() + " is not supported.");
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		return singleErrorList;
	}

	@Override
	public List<FieldError> getFieldErrors() {
		return Collections.emptyList();
	}

	@Override
	public Object getFieldValue(String field) {
		throw new IllegalArgumentException("No field [" + field + "] - " + SingleGlobalError.class.getSimpleName() + " supports no fields");
	}
}