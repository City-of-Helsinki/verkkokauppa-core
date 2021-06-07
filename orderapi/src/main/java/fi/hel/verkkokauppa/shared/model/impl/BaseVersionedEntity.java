package fi.hel.verkkokauppa.shared.model.impl;

import fi.hel.verkkokauppa.shared.model.Versioned;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseVersionedEntity extends BaseEntity implements Versioned {

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	private Instant createdAt;

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	private Instant updatedAt;
}
