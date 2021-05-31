package fi.helshop.microservice.recurringorder.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidationConfiguration {

	@Bean
	public Validator beanValidator() {
		return new LocalValidatorFactoryBean();
	}
}
