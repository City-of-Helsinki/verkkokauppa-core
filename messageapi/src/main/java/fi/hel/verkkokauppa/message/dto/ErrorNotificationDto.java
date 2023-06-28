package fi.hel.verkkokauppa.message.dto;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class ErrorNotificationDto {
    private String message;
    private String cause;
}
