package fi.hel.verkkokauppa.history;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication(exclude = {
		JmsAutoConfiguration.class,
		ActiveMQAutoConfiguration.class,
})
@EnableElasticsearchRepositories
@ComponentScan({
		"fi.hel.verkkokauppa.history",
		"fi.hel.verkkokauppa.history.configuration",
		"fi.hel.verkkokauppa.common.elastic",
		"fi.hel.verkkokauppa.common.error",
		"fi.hel.verkkokauppa.common.sentry"
})
public class HistoryApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HistoryApiApplication.class, args);
	}

}
