package fi.hel.verkkokauppa.message.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.message.constants.ApiUrls;
import fi.hel.verkkokauppa.message.dto.ErrorNotificationDto;
import fi.hel.verkkokauppa.message.dto.MessageDto;
import fi.hel.verkkokauppa.message.enums.MessageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import fi.hel.verkkokauppa.message.model.Message;
import fi.hel.verkkokauppa.message.service.MessageService;

import javax.validation.Valid;

@RestController
@Validated
public class MessageController {
    private final Logger log = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService service;

    @Autowired
    private SendNotificationService sendNotificationService;

    @PostMapping(value = ApiUrls.MESSAGE_ROOT + "/send/email", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Message> sendMessage(@RequestBody @Valid MessageDto messageDto
    ) {
        try {
            Message message = service.createSendableEmailMessage(messageDto);
            service.sendEmail(message);
            return new ResponseEntity<>(message, HttpStatus.CREATED);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Sending message failed, id: " + messageDto.getId(), e);
            Error error = new Error("failed-to-send-message", "failed to send message with id [" + messageDto.getId() + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @PostMapping(value = ApiUrls.MESSAGE_ROOT + "/send/errorNotification", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendErrorNotification(@RequestBody ErrorNotificationDto dto) {
        try {
            sendNotificationService.sendErrorNotification(dto.getMessage(), dto.getCause());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Sending error notification failed {}", dto);
            Error error = new Error("failed-to-send-error-notification", "failed to send error notification");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }
}
