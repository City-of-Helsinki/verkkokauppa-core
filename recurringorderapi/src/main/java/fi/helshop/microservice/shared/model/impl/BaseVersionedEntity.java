package fi.helshop.microservice.shared.model.impl;

import fi.helshop.microservice.shared.model.Versioned;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseVersionedEntity extends BaseEntity implements Versioned {

	private Instant createdAt;
	private Instant UpdatedAt;
}
