package fi.hel.verkkokauppa.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({
        "fi.hel.verkkokauppa.message",
        "fi.hel.verkkokauppa.common.configuration",
        "fi.hel.verkkokauppa.common.queue.config",
        "fi.hel.verkkokauppa.common.queue.error",
        "fi.hel.verkkokauppa.common.queue.service",
        "fi.hel.verkkokauppa.common.queue",
        "fi.hel.verkkokauppa.common.rest",
        "fi.hel.verkkokauppa.common.sentry"
})
public class MessageApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageApiApplication.class, args);
    }

}
