package fi.helshop.microservice.shared.model.impl;

import fi.helshop.microservice.shared.model.Identifiable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseEntity implements Identifiable {

	@Id
	private String id;
}
