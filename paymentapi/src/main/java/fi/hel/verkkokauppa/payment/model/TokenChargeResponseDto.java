package fi.hel.verkkokauppa.payment.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class TokenChargeResponseDto {

	private String accountID;
	private String accessRightTemplate;
	private Integer Quantity;
	private String saleEventId;
	private Instant loadingTimestamp;
	private Instant chargingTimestamp;
	private Instant validationTimestamp;
	private Instant validationExpiration;
	private Instant chargingExpiration;
	private Instant accessRightExpiration;
	private String currentLocation;
	private boolean active;
	private Integer sharedQuantity;
	private String pin;
	private Integer validationsToday;
	private Integer validationsThisWeek;
	// TALPA order item id (row id)
	private String orderItemId;
	private String TokenId;
	private String TokenName;
	private String tokenQRCodeUrl;

}
