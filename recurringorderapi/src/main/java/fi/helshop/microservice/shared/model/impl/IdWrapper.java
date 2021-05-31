package fi.helshop.microservice.shared.model.impl;

import java.io.Serializable;

public final class IdWrapper extends BaseIdentifiableDto implements Serializable {

	private static final long serialVersionUID = -2540103383315455734L;

	public IdWrapper() {
	}

	public IdWrapper(String id) {
		super(id);
	}
}
