package fi.hel.verkkokauppa.message.service;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.message.dto.MessageDto;
import fi.hel.verkkokauppa.message.enums.MessageTypes;
import fi.hel.verkkokauppa.message.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Base64;
import java.util.Map;

@Component
public class MessageService {

    private final Logger log = LoggerFactory.getLogger(MessageService.class);
    @Autowired
    private Environment env;

    @Autowired
    private JavaMailSender javaMailSender;

    public Message createSendableEmailMessage(MessageDto messageDto) {
        return new Message(
                UUIDGenerator.generateType4UUID() + ":" + messageDto.getId(),
                messageDto.getBody(),
                messageDto.getReceiver(),
                env.getRequiredProperty("email.bcc"),
                messageDto.getHeader(),
                messageDto.getId(),
                MessageTypes.EMAIL,
                messageDto.getAttachments()
        );
    }

    public void sendEmail(Message message) throws MailException, MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        boolean multipart = message.getAttachments().size() > 0;
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, "utf-8");

        // split string of email addresses delimited with comma
        String[] receivers = message.getSendTo().split(",");
        helper.setTo(receivers);

        helper.setText(message.getMessageText(), true);
        helper.setBcc(env.getRequiredProperty("email.bcc"));
        helper.setSubject(message.getHeader());
        helper.setFrom(message.getFrom());

        for (Map.Entry<String, String> entry : message.getAttachments().entrySet()) {
            byte[] attachment = Base64.getDecoder().decode(entry.getValue());
            helper.addAttachment(entry.getKey(), new ByteArrayResource(attachment));
        }

        log.info(String.valueOf(mimeMessage));
        javaMailSender.send(mimeMessage);
    }

}
