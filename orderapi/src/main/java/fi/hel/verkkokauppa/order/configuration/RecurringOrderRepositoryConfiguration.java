package fi.hel.verkkokauppa.order.configuration;

import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

public class RecurringOrderRepositoryConfiguration {

	@Configuration
	public static class RecurringOrderJpaRepositoryConfiguration {}
}
