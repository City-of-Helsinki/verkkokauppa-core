package fi.helshop.microservice.shared.model;

import java.time.Instant;

public interface Versioned {

	public Instant getCreatedAt();
	public Instant getUpdatedAt();
}
