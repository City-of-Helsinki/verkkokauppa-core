package fi.helshop.microservice.recurringorder.configuration;

import fi.helshop.microservice.shared.repository.jpa.BaseRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

public class RecurringOrderRepositoryConfiguration {

	@Configuration
	@EnableElasticsearchRepositories(basePackages = "fi.helshop.microservice.recurringorder.repository.jpa", repositoryBaseClass = BaseRepositoryImpl.class)
	public static class RecurringOrderJpaRepositoryConfiguration {}
}
