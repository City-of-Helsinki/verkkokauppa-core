package fi.hel.verkkokauppa.message.service;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.message.dto.MessageDto;
import fi.hel.verkkokauppa.message.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.mail.host=127.0.0.1",
        "spring.mail.username=donotreply.checkout@hel.fi",
        "spring.mail.password=",
        "spring.mail.port=1026",
        "spring.mail.properties.mail.smtp.auth=",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
})
@Slf4j
public class MessageServiceTest {
    // bind the above RANDOM_PORT
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment env;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final Integer PORT_SMTP = 1026;

    public static SimpleSmtpServer dumbster;

    @Before
    public void testInit() throws IOException {
        dumbster = SimpleSmtpServer.start(PORT_SMTP);
    }

    @After
    public void testCleanup() {
        dumbster.close();
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        return javaMailSender;
    }

    /**
     * Sends email message by calling /message/send/email post endpoint
     */
    @Test
    public void sendOrderConfirmationEmail() throws Exception {
        final String baseUrl = "http://localhost:" + port + "/message/send/email";
        URI uri = new URI(baseUrl);
        String header = "test@email.com";
        MessageDto messageDto = new MessageDto(
                "orderId",
                "severikupari1+receive@gmail.com",
                header,
                "Body"
        );

        HttpEntity<MessageDto> request = new HttpEntity<>(messageDto);

        ResponseEntity<String> result = this.restTemplate.postForEntity(uri, request, String.class);

        // Verify request succeed
        Assert.assertEquals(HttpStatus.CREATED.value(), result.getStatusCodeValue());
        Message actual = objectMapper.readValue(result.getBody(), Message.class);
        Assert.assertEquals(Message.class, actual.getClass());
        Assert.assertTrue(result.hasBody());

        // Verify sent email values
        List<SmtpMessage> emails = dumbster.getReceivedEmails();

        SmtpMessage email = emails.get(0);
        assertThat(email.getHeaderValue("Subject"), is(header));
        assertThat(email.getBody(), is("Body"));
        assertThat(email.getHeaderValue("To"), is("severikupari1+receive@gmail.com"));
        assertThat(email.getHeaderValue("From"), is(env.getRequiredProperty("spring.mail.username")));
        assertThat(email.getHeaderValue("Subject"), is("test@email.com"));
        assertThat(email.getHeaderValue("Content-Type"), is("text/html;charset=utf-8"));

    }

    /**
     * Sends email message by calling /message/send/email post endpoint
     */
    @Test
    public void sendMultipleOrderConfirmationEmails() throws Exception {
        final String baseUrl = "http://localhost:" + port + "/message/send/email";
        URI uri = new URI(baseUrl);
        String header = "test@email.com";
        MessageDto messageDto = new MessageDto(
                "orderId",
                "jarkko.amb@gmail.com,jarkkoamb@gmail.com",
                header,
                "Body"
        );

        HttpEntity<MessageDto> request = new HttpEntity<>(messageDto);

        ResponseEntity<String> result = this.restTemplate.postForEntity(uri, request, String.class);

        // Verify request succeed
        Assert.assertEquals(HttpStatus.CREATED.value(), result.getStatusCodeValue());
        Message actual = objectMapper.readValue(result.getBody(), Message.class);
        Assert.assertEquals(Message.class, actual.getClass());
        Assert.assertTrue(result.hasBody());

        // Verify sent email values
        List<SmtpMessage> emails = dumbster.getReceivedEmails();

        SmtpMessage email = emails.get(0);
        assertThat(email.getHeaderValue("Subject"), is(header));
        assertThat(email.getBody(), is("Body"));
        assertThat(email.getHeaderValue("To"), containsString("jarkko.amb@gmail.com"));
        assertThat(email.getHeaderValue("To"), containsString("jarkkoamb@gmail.com"));
        assertThat(email.getHeaderValue("From"), is(env.getRequiredProperty("spring.mail.username")));
        assertThat(email.getHeaderValue("Subject"), is("test@email.com"));
        assertThat(email.getHeaderValue("Content-Type"), is("text/html;charset=utf-8"));
    }
}