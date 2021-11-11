package fi.hel.verkkokauppa.productmapping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Configures product and serviceConfiguration and serviceMappings
 */
@SpringBootApplication
@EnableElasticsearchRepositories
@ComponentScan({"fi.hel.verkkokauppa.productmapping", "fi.hel.verkkokauppa.common","fi.hel.verkkokauppa.common.configuration"})
public class ProductmappingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductmappingApplication.class, args);
	}

}
