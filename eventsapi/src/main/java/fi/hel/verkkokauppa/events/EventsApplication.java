package fi.hel.verkkokauppa.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@ComponentScan({
		"fi.hel.verkkokauppa.events",
		"fi.hel.verkkokauppa.common.events",
		"fi.hel.verkkokauppa.common.error",
		"fi.hel.verkkokauppa.common.rest",
		"fi.hel.verkkokauppa.common.queue",
		"fi.hel.verkkokauppa.common.configuration",
		"fi.hel.verkkokauppa.common.sentry"
})
public class EventsApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventsApplication.class, args);
	}

}
