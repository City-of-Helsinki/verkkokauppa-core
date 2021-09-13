package fi.hel.verkkokauppa.message.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.constraints.*;

/**
 *  sähköpostin lähettäjän,
 *  vastaanottajan,
 *  sähköpostin otsikon
 *  ja sisällön,
 *  sekä toimittaa sähköpostin relay.helsinki.fi
 *  kautta vastaanottajan sähköpostiosoitteeseen
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MessageDto extends BaseDto {
    @NotEmpty
    private String orderId;

    private String sender;

    @NotEmpty
    @Email
    private String receiver;

    @NotBlank
    private String header;

    @NotBlank
    private String body;

    public MessageDto(String orderId, String receiver, String header, String body) {
        this.orderId = orderId;
        this.receiver = receiver;
        this.header = header;
        this.body = body;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }
}
