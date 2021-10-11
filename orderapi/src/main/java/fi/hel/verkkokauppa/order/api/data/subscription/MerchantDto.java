package fi.hel.verkkokauppa.order.api.data.subscription;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class MerchantDto implements Serializable {

	private static final long serialVersionUID = -1979182823636136632L;

	private String name;
	private String namespace;
}
