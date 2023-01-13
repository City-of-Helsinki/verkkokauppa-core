package fi.hel.verkkokauppa.order.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.order.mapper.OrderPaymentMethodMapper;
import fi.hel.verkkokauppa.order.model.OrderPaymentMethod;
import fi.hel.verkkokauppa.order.repository.jpa.OrderPaymentMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderPaymentMethodService {

    private final OrderService orderService;
    private final OrderPaymentMethodRepository orderPaymentMethodRepository;
    private final OrderPaymentMethodMapper orderPaymentMethodMapper;

    @Autowired
    OrderPaymentMethodService(
            OrderService orderService,
            OrderPaymentMethodRepository orderPaymentMethodRepository,
            OrderPaymentMethodMapper orderPaymentMethodMapper
    ) {
        this.orderService = orderService;
        this.orderPaymentMethodRepository = orderPaymentMethodRepository;
        this.orderPaymentMethodMapper = orderPaymentMethodMapper;
    }

    public OrderPaymentMethodDto upsertOrderPaymentMethod(OrderPaymentMethodDto dto) {
        orderService.findByIdValidateByUser(dto.getOrderId(), dto.getUserId());

        Optional<OrderPaymentMethod> orderPaymentMethodOpt = orderPaymentMethodRepository.findByOrderId(dto.getOrderId()).stream().findFirst();
        OrderPaymentMethod paymentMethodToSave;
        if (orderPaymentMethodOpt.isPresent()) {
            try {
                paymentMethodToSave = orderPaymentMethodMapper.updateFromDtoToModel(orderPaymentMethodOpt.get(), dto);
            } catch (JsonProcessingException e) {
                throw new CommonApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        new Error("failed-to-set-order-payment-method-properties", "failed to set order payment method properties")
                );
            }
        } else {
            paymentMethodToSave = orderPaymentMethodMapper.fromDto(dto);
        }
        OrderPaymentMethod saved = orderPaymentMethodRepository.save(paymentMethodToSave);

        return orderPaymentMethodMapper.toDto(saved);
    }

    public Optional<OrderPaymentMethod> getPaymentMethodForOrder(String orderId) {
        return orderPaymentMethodRepository.findByOrderId(orderId).stream().findFirst();
    }
}
