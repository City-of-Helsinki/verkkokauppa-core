package fi.hel.verkkokauppa.message.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.constraints.*;
import java.util.HashMap;
import java.util.Map;

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
    private String id;

    private String sender;

    @NotEmpty
    @Email
    private String receiver;

    @NotBlank
    private String header;

    @NotBlank
    private String body;

    @NotNull
    private Map<String, String> attachments;

    public MessageDto(String id, String receiver, String header, String body) {
        this.id = id;
        this.receiver = receiver;
        this.header = header;
        this.body = body;
        this.attachments = new HashMap<>();
    }

    public String getId() {
        return id;
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

    public Map<String, String> getAttachments() { return attachments; }
}
