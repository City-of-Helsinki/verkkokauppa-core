package fi.hel.verkkokauppa.order.api.data.order;

import java.time.LocalDate;

public class RecurrenceInfoDto {

	private LocalDate startDate; // TODO: aika myös?
	private String periodUnit;
	private Long periodFrequency;
	// TODO: muut kentät?
}
