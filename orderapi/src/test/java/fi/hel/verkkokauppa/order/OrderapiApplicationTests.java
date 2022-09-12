package fi.hel.verkkokauppa.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
@EnableAutoConfiguration(exclude = {
		ActiveMQAutoConfiguration.class,
		KafkaAutoConfiguration.class,
		ReactiveElasticsearchRepositoriesAutoConfiguration.class,
		ReactiveElasticsearchRestClientAutoConfiguration.class
})
class OrderapiApplicationTests {

	@Test
	void contextLoads() {
	}

}