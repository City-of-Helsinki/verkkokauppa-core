package fi.helshop.microservice.recurringorder.model.recurringorder;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

@Getter
@Setter
public class Merchant implements Serializable {

	private static final long serialVersionUID = -1979182823636136632L;

	@Field(type = FieldType.Keyword)
	private String namespace;

	@Field(type = FieldType.Text)
	private String name;
}
