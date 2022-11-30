package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.model.PaymentMethodModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PaymentMethodRepository extends ElasticsearchRepository<PaymentMethodModel, String> {

    List<PaymentMethodModel> findByGateway(PaymentGatewayEnum gateway);
    List<PaymentMethodModel> findByCode(String code);
    long deleteByCode(String code);
}
