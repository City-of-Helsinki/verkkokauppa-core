package fi.hel.verkkokauppa.order.test.utils;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.accounting.CreateRefundAccountingRequestDto;
import fi.hel.verkkokauppa.order.api.data.accounting.ProductAccountingDto;
import fi.hel.verkkokauppa.order.constants.RefundAccountingStatusEnum;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import fi.hel.verkkokauppa.order.model.accounting.RefundItemAccounting;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.repository.jpa.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class AccountingTestUtils extends DummyData {
    @Autowired
    public OrderRepository orderRepository;

    @Autowired
    public OrderItemRepository orderItemRepository;

    @Autowired
    public RefundRepository refundRepository;

    @Autowired
    public RefundItemRepository refundItemRepository;

    @Autowired
    public OrderAccountingRepository orderAccountingRepository;

    @Autowired
    public RefundAccountingRepository refundAccountingRepository;

    @Autowired
    public OrderItemAccountingRepository orderItemAccountingRepository;

    @Autowired
    public RefundItemAccountingRepository refundItemAccountingRepository;

    protected ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    protected ArrayList<String> toBeDeletedOrderItemById = new ArrayList<>();
    protected ArrayList<String> toBeDeletedOrderAccountingById = new ArrayList<>();
    protected ArrayList<String> toBeDeletedOrderItemAccountingById = new ArrayList<>();
    protected ArrayList<String> toBeDeletedRefundById = new ArrayList<>();
    protected ArrayList<String> toBeDeletedRefundItemById = new ArrayList<>();
    protected ArrayList<String> toBeDeletedRefundAccountingById = new ArrayList<>();
    protected ArrayList<String> toBeDeletedRefundItemAccountingById = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedOrderById.forEach(orderId -> orderRepository.deleteById(orderId));
            toBeDeletedOrderItemById.forEach(orderItemId -> orderItemRepository.deleteById(orderItemId));
            toBeDeletedRefundById.forEach(refundId -> refundRepository.deleteById(refundId));
            toBeDeletedRefundItemById.forEach(refundId -> refundItemRepository.deleteById(refundId));
            toBeDeletedOrderAccountingById.forEach(id -> orderAccountingRepository.deleteById(id));
            toBeDeletedRefundAccountingById.forEach(id -> refundAccountingRepository.deleteById(id));
            toBeDeletedOrderItemAccountingById.forEach(id -> orderItemAccountingRepository.deleteById(id));
            toBeDeletedRefundItemAccountingById.forEach(id -> refundItemAccountingRepository.deleteById(id));
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedOrderItemById = new ArrayList<>();
            toBeDeletedOrderAccountingById = new ArrayList<>();
            toBeDeletedOrderItemAccountingById = new ArrayList<>();
            toBeDeletedRefundById = new ArrayList<>();
            toBeDeletedRefundItemById = new ArrayList<>();
            toBeDeletedRefundAccountingById = new ArrayList<>();
            toBeDeletedRefundItemAccountingById = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedOrderItemById = new ArrayList<>();
            toBeDeletedOrderAccountingById = new ArrayList<>();
            toBeDeletedOrderItemAccountingById = new ArrayList<>();
            toBeDeletedRefundById = new ArrayList<>();
            toBeDeletedRefundItemById = new ArrayList<>();
            toBeDeletedRefundAccountingById = new ArrayList<>();
            toBeDeletedRefundItemAccountingById = new ArrayList<>();
        }
    }

    public Order createTestOrder() {
        Order order = generateDummyOrder();
        order.setOrderId(UUID.randomUUID().toString());
        order = orderRepository.save(order);
        toBeDeletedOrderById.add(order.getOrderId());
        return order;
    }

    public OrderItem createTestOrderItem(Order order) {
        String orderItemId = UUIDGenerator.generateType4UUID().toString();
        //String orderId, String productId, String productName, Integer quantity, String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross
        OrderItem orderItem = new OrderItem(
                orderItemId,
                order.getOrderId(),
                "9876",
                "8a8674ed-1ae2-3ca9-a93c-036478b2a032",
                "productName",
                "productLabel",
                "productDescription",
                1,
                "unit",
                "100",
                "100",
                "100",
                "0",
                order.getPriceNet(),
                order.getPriceVat(),
                "100",
                order.getPriceNet(),
                order.getPriceVat(),
                "100",
                null,
                null,
                null,
                null,
                null,
                null
        );
        orderItemRepository.save(orderItem);
        toBeDeletedOrderItemById.add(orderItemId);

        return orderItem;
    }

    public OrderItem createFreeTestOrderItem(Order order, String price) {
        String orderItemId = UUIDGenerator.generateType4UUID().toString();
        //String orderId, String productId, String productName, Integer quantity, String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross
        OrderItem orderItem = new OrderItem(
                orderItemId,
                order.getOrderId(),
                "9876",
                "free-product-id",
                "freeName",
                "freeLabel",
                "freeDescription",
                1,
                "unit",
                price,
                price,
                price,
                "0",
                price,
                price,
                price,
                price,
                price,
                price,
                null,
                null,
                null,
                null,
                null,
                null
        );
        orderItemRepository.save(orderItem);
        toBeDeletedOrderItemById.add(orderItemId);

        return orderItem;
    }

    public OrderAccounting createTestOrderAccounting(String orderId) {
        OrderAccounting orderAccounting = new OrderAccounting();
        orderAccounting.setOrderId(orderId);
        orderAccounting.setCreatedAt(DateTimeUtil.getFormattedDateTime().minusDays(1));
        orderAccounting = orderAccountingRepository.save(orderAccounting);
        toBeDeletedOrderAccountingById.add(orderAccounting.getOrderId());

        return orderAccounting;
    }

    public OrderItemAccounting createTestOrderItemAccounting(String orderId, String priceGross, String priceNet, String priceVat,
                                                             String companyCode, String mainLedgerAccount, String vatCode,
                                                             String internalOrder, String profitCenter, String balanceProfitCenter,
                                                             String project, String operationArea) {
        OrderItemAccounting orderItemAccounting = new OrderItemAccounting(
                UUID.randomUUID().toString(),
                orderId,
                priceGross,
                priceNet,
                priceVat,
                companyCode,
                mainLedgerAccount,
                vatCode,
                internalOrder,
                profitCenter,
                balanceProfitCenter,
                project,
                operationArea);

        orderItemAccounting = orderItemAccountingRepository.save(orderItemAccounting);
        toBeDeletedOrderItemAccountingById.add(orderItemAccounting.getOrderItemId());

        return orderItemAccounting;
    }

    public Refund createTestRefund(String orderId) {
        Refund refund = generateDummyRefund(orderId);
        refund.setRefundId(UUID.randomUUID().toString());
        refund = refundRepository.save(refund);
        toBeDeletedRefundById.add(refund.getRefundId());
        return refund;
    }

    public Refund createTestRefund(String orderId, String priceNet, String priceVat) {
        Refund refund = generateDummyRefund(orderId);
        refund.setRefundId(UUID.randomUUID().toString());

        refund.setPriceNet(priceNet);
        refund.setPriceVat(priceVat);
        double priceTotal = Double.parseDouble(priceNet) + Double.parseDouble(priceVat);
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        refund.setPriceTotal(decimalFormat.format(priceTotal).replace(".", ","));

        refund = refundRepository.save(refund);
        toBeDeletedRefundById.add(refund.getRefundId());
        return refund;
    }

    public RefundItem createTestRefundItem(Refund refund) {
        RefundItem refundItem = generateDummyRefundItem(refund);
        refundItem = refundItemRepository.save(refundItem);
        toBeDeletedRefundItemById.add(refundItem.getRefundItemId());
        return refundItem;
    }

    public RefundItem createTestRefundItem(Refund refund, String priceGross, String priceNet, String priceVat) {
        RefundItem refundItem = generateDummyRefundItem(refund);
        refundItem.setPriceGross(priceGross);
        refundItem.setPriceNet(priceNet);
        refundItem.setPriceVat(priceVat);
        refundItem = refundItemRepository.save(refundItem);
        toBeDeletedRefundItemById.add(refundItem.getRefundItemId());
        return refundItem;
    }

    public CreateRefundAccountingRequestDto createRefundAccountingRequest(Refund refund, RefundItem item) {
        CreateRefundAccountingRequestDto requestDto = new CreateRefundAccountingRequestDto();
        requestDto.setRefundId(refund.getRefundId());
        requestDto.setOrderId(refund.getOrderId());

        List<ProductAccountingDto> dtos = new ArrayList<>();

        ProductAccountingDto dto = new ProductAccountingDto();
        dto.setProductId(item.getProductId());
        dto.setProject("project");
        dto.setMainLedgerAccount("MainLedgerAccount");
        dto.setCompanyCode("1234");
        dto.setOperationArea("operationArea");
        dto.setInternalOrder("1234");
        dto.setBalanceProfitCenter("balanceProfitCenter");
        dto.setProfitCenter("profitCenter");
        dto.setVatCode("10");

        dtos.add(dto);

        requestDto.setDtos(dtos);

        return requestDto;
    }

    public Refund setTestRefundAccountingStatus(String refundId, RefundAccountingStatusEnum accountingStatus) {
        Optional<Refund> returnedRefund = refundRepository.findById(refundId);
        Refund refund = returnedRefund.get();
        if (refund != null) {
            refund.setAccountingStatus(accountingStatus);
            refund = refundRepository.save(refund);
        }
        return refund;
    }

    public RefundAccounting createTestRefundAccounting(String refundId, String orderId) {
        RefundAccounting refundAccounting = new RefundAccounting();
        refundAccounting.setRefundId(refundId);
        refundAccounting.setOrderId(orderId);
        refundAccounting.setCreatedAt(DateTimeUtil.getFormattedDateTime().minusDays(1));
        refundAccounting = refundAccountingRepository.save(refundAccounting);
        toBeDeletedRefundAccountingById.add(refundAccounting.getRefundId());

        return refundAccounting;
    }

    public RefundItemAccounting createTestRefundItemAccounting(String refundId, String orderId, String priceGross, String priceNet, String priceVat,
                                                               String companyCode, String mainLedgerAccount, String vatCode,
                                                               String internalOrder, String profitCenter, String balanceProfitCenter,
                                                               String project, String operationArea) {
        RefundItemAccounting refundItemAccounting = new RefundItemAccounting(
                UUID.randomUUID().toString(),
                refundId,
                orderId,
                priceGross,
                priceNet,
                priceVat,
                companyCode,
                mainLedgerAccount,
                vatCode,
                internalOrder,
                profitCenter,
                balanceProfitCenter,
                project,
                operationArea);

        refundItemAccounting = refundItemAccountingRepository.save(refundItemAccounting);
        toBeDeletedRefundItemAccountingById.add(refundItemAccounting.getRefundItemId());

        return refundItemAccounting;
    }

}
