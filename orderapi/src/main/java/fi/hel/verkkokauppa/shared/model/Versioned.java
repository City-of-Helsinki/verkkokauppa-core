package fi.hel.verkkokauppa.shared.model;

import java.time.Instant;

public interface Versioned {

	public Instant getCreatedAt();
	public Instant getUpdatedAt();
}
