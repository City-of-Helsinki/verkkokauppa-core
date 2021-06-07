package fi.hel.verkkokauppa.order.configuration;

import fi.hel.verkkokauppa.shared.mapper.DefaultMapper;
import fi.hel.verkkokauppa.shared.mapper.Mapper;
import org.modelmapper.ModelMapper;
import org.modelmapper.Provider;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spring.SpringIntegration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfiguration {

	@Bean
	public Provider<?> modelMapperProvider(BeanFactory beanFactory) {
		return SpringIntegration.fromSpring(beanFactory);
	}

	@Bean
	public ModelMapper modelMapper() {
		final ModelMapper modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		return modelMapper;
	}

	@Bean
	public Mapper mapper() {
		return new DefaultMapper(modelMapper());
	}
}
