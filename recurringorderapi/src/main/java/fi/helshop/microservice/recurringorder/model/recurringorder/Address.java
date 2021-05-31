package fi.helshop.microservice.recurringorder.model.recurringorder;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Address implements Serializable {

	private static final long serialVersionUID = -1979182823636136632L;

	private String street;
	private String postalCode;
	// TODO: pölli magentosta loput

}
