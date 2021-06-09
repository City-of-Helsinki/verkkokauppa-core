package fi.hel.verkkokauppa.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories
@ComponentScan({"fi.hel.verkkokauppa.product", "fi.hel.verkkokauppa.common"})
public class ProductapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductapiApplication.class, args);
	}

}
