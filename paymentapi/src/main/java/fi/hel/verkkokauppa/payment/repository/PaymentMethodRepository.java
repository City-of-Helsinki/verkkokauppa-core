package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.model.PaymentMethodModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PaymentMethodRepository extends ElasticsearchRepository<PaymentMethodModel, String> {

    List<PaymentMethodModel> findByGateway(GatewayEnum gateway);
    List<PaymentMethodModel> findByCode(String code);
    long deleteByCode(String code);
}
