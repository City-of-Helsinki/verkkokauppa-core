package fi.hel.verkkokauppa.message.service;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.message.dto.MessageDto;
import fi.hel.verkkokauppa.message.enums.MessageTypes;
import fi.hel.verkkokauppa.message.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class MessageService {

    private final Logger log = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    public Message createSendableEmailMessage(MessageDto messageDto) {
        return new Message(
                UUIDGenerator.generateType3UUIDString(MessageTypes.EMAIL.toString(), messageDto.getOrderId()),
                messageDto.getBody(),
                messageDto.getReceiver(),
                messageDto.getSender(),
                messageDto.getHeader(),
                messageDto.getOrderId(),
                MessageTypes.EMAIL
        );
    }

    public void sendEmail(Message message) throws MailException, MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        helper.setText(message.getMessageText(),true);
        helper.setTo(message.getSendTo());
        helper.setSubject(message.getHeader());
        helper.setFrom(message.getFrom());

        log.info(String.valueOf(mimeMessage));
        javaMailSender.send(mimeMessage);
    }

}
