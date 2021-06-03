package fi.hel.verkkokauppa.order.api.data.recurringorder;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AddressDto implements Serializable {

	private static final long serialVersionUID = -1979182823636136632L;

	private String id;
	private String street;
	private String city;
	private String postalCode;
	private String state;
	private String country;
}
