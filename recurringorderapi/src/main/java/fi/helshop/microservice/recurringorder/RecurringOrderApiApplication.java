package fi.helshop.microservice.recurringorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories
public class RecurringOrderApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(RecurringOrderApiApplication.class, args);
	}
}
