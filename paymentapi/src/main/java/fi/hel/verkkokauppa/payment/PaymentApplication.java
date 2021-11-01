package fi.hel.verkkokauppa.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories
@ComponentScan({"fi.hel.verkkokauppa.payment", "fi.hel.verkkokauppa.common.elastic", "fi.hel.verkkokauppa.common.error", "fi.hel.verkkokauppa.common.events"})
public class PaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentApplication.class, args);
	}

}
