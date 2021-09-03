package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipRowDto;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipRowTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemAccountingTransformer;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlipRow;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingSlipRepository;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingSlipRowRepository;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountingSlipService {

    private Logger log = LoggerFactory.getLogger(AccountingSlipService.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderAccountingService orderAccountingService;

    @Autowired
    private OrderItemAccountingService orderItemAccountingService;

    @Autowired
    private AccountingSlipRepository accountingSlipRepository;

    @Autowired
    private AccountingSlipRowRepository accountingSlipRowRepository;

    public AccountingSlip getAccountingSlip(String accountingSlipId) {
        Optional<AccountingSlip> mapping = accountingSlipRepository.findById(accountingSlipId);

        if (mapping.isPresent())
            return mapping.get();

        throw new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("accounting-slip-not-found-from-backend", "accounting slip with accountingSlipId [" + accountingSlipId + "] not found from backend")
        );
    }

    public List<AccountingSlipRow> getAccountingSlipRows(String accountingSlipId) {
        List<AccountingSlipRow> rows = accountingSlipRowRepository.findByAccountingSlipId(accountingSlipId);

        if (!rows.isEmpty()) {
            return rows;
        }

        throw new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("accounting-slip-rows-not-found-from-backend", "accounting slip rows with accountingSlipId [" + accountingSlipId + "] not found from backend")
        );
    }

    public AccountingSlipDto transformAccountingSlipWithRowsToDto(AccountingSlip accounting, List<AccountingSlipRow> accountingSlipRows) {
        AccountingSlipDto accountingSlipDto = new AccountingSlipTransformer().transformToDto(accounting);
        List<AccountingSlipRowDto> accountingSlipRowDtos = new ArrayList<>();
        accountingSlipRows.forEach(row -> accountingSlipRowDtos.add(new AccountingSlipRowTransformer().transformToDto(row)));
        accountingSlipDto.setRows(accountingSlipRowDtos);

        return accountingSlipDto;
    }

    public List<AccountingSlipDto> createAccountingSlips(Map<String, List<String>> accountingIdsByDate) {
        List<AccountingSlipDto> accountingSlips = new ArrayList<>();

        for (Map.Entry<String, List<String>> accountingsForDate : accountingIdsByDate.entrySet()) {
            AccountingSlipDto accountingSlipDto = createAccountingSlipForDate(accountingsForDate);
            accountingSlips.add(accountingSlipDto);
        }
        return accountingSlips;
    }

    public Map<String, List<String>> groupAccountingsByDate(List<Order> ordersToAccount) {
        Map<String, List<String>> map = new HashMap<>();

        List<String> orderIds = ordersToAccount.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());

        List<OrderAccounting> orderAccountings = orderAccountingService.getOrderAccountings(orderIds);

        for (OrderAccounting orderAccounting : orderAccountings) {
            String createdAt = orderAccounting.getCreatedAt();

            if (LocalDate.parse(createdAt).isBefore(LocalDate.now())) {
                List<String> accountingsForDate = map.get(createdAt);

                if (accountingsForDate == null) {
                    ArrayList<String> accountings = new ArrayList<>();
                    accountings.add(orderAccounting.getOrderId());
                    map.put(createdAt, accountings);
                } else {
                    accountingsForDate.add(orderAccounting.getOrderId());
                }
            }
        }

        return map;
    }

    private AccountingSlipDto createAccountingSlipForDate(Map.Entry<String, List<String>> accountingsForDate) {
        List<OrderItemAccountingDto> itemAccountings = getOrderItemAccountingsForDate(accountingsForDate);

        // Summataan hinnat yksittäisen tiliöinnin tasolle
        Collection<OrderItemAccountingDto> summedItemAccountings = itemAccountings.stream()
                .collect(Collectors.toMap(OrderItemAccountingDto::createKey, Function.identity(), (OrderItemAccountingDto::merge)))
                .values();

        String postingDate = accountingsForDate.getKey();
        String documentDate = DateTimeUtil.getDate();
        String accountingSlipId = UUIDGenerator.generateType3UUIDString(postingDate, documentDate);

        List<AccountingSlipRowDto> rows = new ArrayList<>();
        int rowNumber = 1;

        for (OrderItemAccountingDto summedItemAccounting : summedItemAccountings) {
            String accountingSlipRowId = UUIDGenerator.generateType3UUIDString(accountingSlipId, Integer.toString(rowNumber));

            AccountingSlipRowDto accountingSlipRowDto = new AccountingSlipRowDto(
                    accountingSlipRowId,
                    accountingSlipId,
                    formatSum(summedItemAccounting.getPriceGrossAsDouble()),
                    formatSum(summedItemAccounting.getPriceNetAsDouble()),
                    summedItemAccounting.getMainLedgerAccount(),
                    summedItemAccounting.getVatCode(),
                    summedItemAccounting.getInternalOrder(),
                    summedItemAccounting.getProfitCenter(),
                    summedItemAccounting.getProject(),
                    summedItemAccounting.getOperationArea()
            );

            rows.add(accountingSlipRowDto);
            rowNumber++;
        }

        AccountingSlipDto accountingSlipDto = new AccountingSlipDto(
                accountingSlipId,
                documentDate,
                "FILL THIS",
                "VK",
                postingDate,
                "FILL THIS",
                "FILL THIS",
                "EUR",
                rows
        );

        createAccountingWithRows(accountingSlipDto);

        accountingsForDate.getValue().forEach(orderId -> orderService.markAsAccounted(orderId));

        return accountingSlipDto;
    }

    private List<OrderItemAccountingDto> getOrderItemAccountingsForDate(Map.Entry<String, List<String>> accountingsForDate) {
        List<OrderItemAccountingDto> accountingItemsForDate = new ArrayList<>();

        for (String id : accountingsForDate.getValue()) {
            List<OrderItemAccounting> list = orderItemAccountingService.getOrderItemAccountings(id);
            list.forEach(itemAccounting -> accountingItemsForDate.add(new OrderItemAccountingTransformer().transformToDto(itemAccounting)));
        }
        return accountingItemsForDate;
    }

    private void createAccountingWithRows(AccountingSlipDto accountingSlipDto) {
        AccountingSlip accountingSlip = accountingSlipRepository.save(new AccountingSlipTransformer().transformToEntity(accountingSlipDto));

        List<AccountingSlipRowDto> rows = accountingSlipDto.getRows();
        rows.forEach(row -> accountingSlipRowRepository.save(new AccountingSlipRowTransformer().transformToEntity(row)));

        log.debug("created new accounting slip, accountingId: " + accountingSlip.getAccountingSlipId());
    }

    private String formatSum(Double sum) {
        return Double.toString(-sum).replace(".", ",");
    }

}
