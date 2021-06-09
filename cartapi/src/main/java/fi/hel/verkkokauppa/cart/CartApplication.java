package fi.hel.verkkokauppa.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories
@ComponentScan({"fi.hel.verkkokauppa.cart", "fi.hel.verkkokauppa.common"})
public class CartApplication {

	public static void main(String[] args) {
		SpringApplication.run(CartApplication.class, args);
	}

}
