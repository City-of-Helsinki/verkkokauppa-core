package fi.hel.verkkokauppa.productmapping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.map.repository.config.EnableMapRepositories;

@SpringBootApplication
@EnableMapRepositories
public class ProductmappingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductmappingApplication.class, args);
	}

}
