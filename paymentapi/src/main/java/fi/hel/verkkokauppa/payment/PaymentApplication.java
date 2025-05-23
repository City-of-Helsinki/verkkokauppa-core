package fi.hel.verkkokauppa.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.config.EnableElasticsearchAuditing;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories
@EnableElasticsearchAuditing
@ComponentScan({
		"fi.hel.verkkokauppa.payment",
		"fi.hel.verkkokauppa.common.elastic",
		"fi.hel.verkkokauppa.common.error",
		"fi.hel.verkkokauppa.common.events",
		"fi.hel.verkkokauppa.common.history",
		"fi.hel.verkkokauppa.common.rest",
		"fi.hel.verkkokauppa.common.configuration",
		"fi.hel.verkkokauppa.common.queue",
		"fi.hel.verkkokauppa.common.sentry",
		"fi.hel.verkkokauppa.common.service"
})
public class PaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentApplication.class, args);
	}

}
