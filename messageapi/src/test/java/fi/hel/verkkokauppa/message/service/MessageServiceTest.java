package fi.hel.verkkokauppa.message.service;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.message.dto.MessageDto;
import fi.hel.verkkokauppa.message.model.Message;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.net.URI;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MessageServiceTest {
    /**
     * Elasticsearch version which should be used for the Tests
     */
    private static final String ELASTICSEARCH_VERSION = "7.9.2";
    private static final DockerImageName ELASTICSEARCH_IMAGE =
            DockerImageName
                    .parse("docker.elastic.co/elasticsearch/elasticsearch")
                    .withTag(ELASTICSEARCH_VERSION);

    // bind the above RANDOM_PORT
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)) {
            // Start the container. This step might take some time...
            container.start();
        }
    }

    private static final Integer PORT_SMTP = 1025;

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
                "severikupari1@gmail.com",
                "severikupari1+receive@gmail.com",
                header,
                "Body"
        );

        HttpEntity<MessageDto> request = new HttpEntity<>(messageDto);

        SimpleSmtpServer dumbster = SimpleSmtpServer.start(PORT_SMTP);

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
        assertThat(email.getHeaderValue("From"), is("severikupari1@gmail.com"));
        assertThat(email.getHeaderValue("Subject"), is("test@email.com"));
        assertThat(email.getHeaderValue("Content-Type"), is( "text/html;charset=utf-8"));

    }

}