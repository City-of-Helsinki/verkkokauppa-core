package fi.hel.verkkokauppa.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
		ReactiveElasticsearchRepositoriesAutoConfiguration.class,
		ReactiveElasticsearchRestClientAutoConfiguration.class
})
class OrderapiApplicationTests {

	@Test
	void contextLoads() {
	}

}
