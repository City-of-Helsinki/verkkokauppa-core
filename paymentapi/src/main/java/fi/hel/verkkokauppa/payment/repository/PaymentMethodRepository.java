package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.model.PaymentMethod;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PaymentMethodRepository extends ElasticsearchRepository<PaymentMethod, String> {

    List<PaymentMethod> findByGateway(GatewayEnum gateway);
    List<PaymentMethod> findByCode(String code);
    long deleteByCode(String code);
}
