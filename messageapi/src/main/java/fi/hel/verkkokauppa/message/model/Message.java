package fi.hel.verkkokauppa.message.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.hel.verkkokauppa.message.enums.MessageTypes;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "messages")
@JsonIgnoreProperties("messageText")
public class Message {
    @Id
    String id;
    @Field(type = FieldType.Text)
    String messageText;
    @Field(type = FieldType.Text)
    String sendTo;
    @Field(type = FieldType.Text)
    String from;
    @Field(type = FieldType.Text)
    String header;
    @Field(type = FieldType.Text)
    String identifierValue;
    @Field(type = FieldType.Text)
    MessageTypes messageType;

    public Message() {
    }

    public Message(String id, String messageText, String sendTo, String from, String header, String identifierValue, MessageTypes messageType) {
        this.id = id;
        this.messageText = messageText;
        this.sendTo = sendTo;
        this.from = from;
        this.header = header;
        this.identifierValue = identifierValue;
        this.messageType = messageType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }

    public MessageTypes getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageTypes messageType) {
        this.messageType = messageType;
    }
}
