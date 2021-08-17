package fi.hel.verkkokauppa.mockproductmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"fi.hel.verkkokauppa.mockproductmanagement", "fi.hel.verkkokauppa.backend"})
public class MockproductmanagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockproductmanagementApplication.class, args);
	}

}
