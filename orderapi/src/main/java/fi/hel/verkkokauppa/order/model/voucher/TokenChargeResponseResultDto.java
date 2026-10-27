package fi.hel.verkkokauppa.order.model.voucher;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class TokenChargeResponseResultDto {

	// TALPA order item id (row id)
	private String orderItemId;
	private String access_right_id;
	private String account_id;
	private String access_right_template_id;
	private Integer quantity;
	private String sale_event_id;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime loading_timestamp;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime  charging_timestamp;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime  validation_timestamp;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime  validation_expiration;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime  charging_expiration;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime  access_right_expiration;
	private String current_location_id;
	private boolean is_active;
	private String shared_quantity_id;
	private String pin;
	private Integer validations_today;
	private Integer validations_this_week;
	private String access_right_template_name;

	private String token_id;
	private String token_name;
	private String token_qr_url;

}
