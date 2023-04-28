package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
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

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountingSlipService {

    private Logger log = LoggerFactory.getLogger(AccountingSlipService.class);

    public static final int REFERENCE_NUMBER_LENGTH = 3;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderAccountingService orderAccountingService;

    @Autowired
    private OrderItemAccountingService orderItemAccountingService;

    @Autowired
    private AccountingExportDataService accountingExportDataService;

    @Autowired
    private AccountingExportService accountingExportService;

    @Autowired
    private AccountingSlipRepository accountingSlipRepository;

    @Autowired
    private AccountingSlipRowRepository accountingSlipRowRepository;

    @Autowired
    private AccountingSearchService accountingSearchService;

    public List<AccountingSlipDto> createAccountingData() throws Exception {
        List<Order> ordersToAccount = accountingSearchService.findNotAccounted();
        Map<LocalDate, List<String>> accountingIdsByDate = groupAccountingsByDate(ordersToAccount);

        // not handling current date
        if (accountingIdsByDate == null || accountingIdsByDate.isEmpty()) {
            log.info("no orders to account");
            return new ArrayList<>();
        }

        return createAccountingSlips(accountingIdsByDate);
    }

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

    public AccountingSlipDto getAccountingSlipDtoWithRows(AccountingSlip accountingSlip) {
        String accountingSlipId = accountingSlip.getAccountingSlipId();
        List<AccountingSlipRow> accountingSlipRows = getAccountingSlipRows(accountingSlipId);

        List<AccountingSlipRowDto> rowDtos = new ArrayList<>();
        accountingSlipRows.forEach(row -> rowDtos.add(new AccountingSlipRowTransformer().transformToDto(row)));

        AccountingSlipDto accountingSlipDto = new AccountingSlipTransformer().transformToDto(accountingSlip);
        accountingSlipDto.setRows(rowDtos);

        return accountingSlipDto;
    }

    public AccountingSlipDto transformAccountingSlipWithRowsToDto(AccountingSlip accounting, List<AccountingSlipRow> accountingSlipRows) {
        AccountingSlipDto accountingSlipDto = new AccountingSlipTransformer().transformToDto(accounting);
        List<AccountingSlipRowDto> accountingSlipRowDtos = new ArrayList<>();
        accountingSlipRows.forEach(row -> accountingSlipRowDtos.add(new AccountingSlipRowTransformer().transformToDto(row)));
        accountingSlipDto.setRows(accountingSlipRowDtos);

        return accountingSlipDto;
    }

    public List<AccountingSlipDto> createAccountingSlips(Map<LocalDate, List<String>> accountingIdsByDate) {
        List<AccountingSlipDto> accountingSlips = new ArrayList<>();

        for (Map.Entry<LocalDate, List<String>> accountingsForDate : accountingIdsByDate.entrySet()) {
            List<AccountingSlipDto> accountingSlipDtos = createAccountingSlipForDate(accountingsForDate);
            accountingSlips.addAll(accountingSlipDtos);
        }
        return accountingSlips;
    }

    public Map<LocalDate, List<String>> groupAccountingsByDate(List<Order> ordersToAccount) {
        Map<LocalDate, List<String>> map = new HashMap<>();

        List<String> orderIds = ordersToAccount.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());

        List<OrderAccounting> orderAccountings = orderAccountingService.getOrderAccountings(orderIds);

        for (OrderAccounting orderAccounting : orderAccountings) {
            LocalDate createdAt = orderAccounting.getCreatedAt().toLocalDate();

            if (createdAt.isBefore(LocalDate.now())) {
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

    private List<AccountingSlipDto> createAccountingSlipForDate(Map.Entry<LocalDate, List<String>> accountingsForDate) {
        List<AccountingSlipDto> accountingSlipDtos = new ArrayList<>();

        LocalDate postingDate = accountingsForDate.getKey();
        int referenceNumber = postingDate.getDayOfYear();

        Map<String, List<OrderItemAccountingDto>> summedItemAccountings = getSummedOrderItemAccountingsForDate(accountingsForDate);

        for (var accountingListForCompanyCode : summedItemAccountings.entrySet()) {
            List<OrderItemAccountingDto> summedItemAccountingsForCompanyCode = accountingListForCompanyCode.getValue();

            String companyCode = accountingListForCompanyCode.getKey();
            String accountingSlipId = UUIDGenerator.generateType3UUIDString(postingDate.toString(), companyCode);

            String headerTextDate = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(postingDate);
            String headertext = "Verkkokauppa " + headerTextDate;

            List<AccountingSlipRowDto> rows = createAccountingSlipRowDtos(summedItemAccountingsForCompanyCode, accountingSlipId, headertext);

            String referenceYear = DateTimeFormatter.ofPattern("yy").format(postingDate);
            String referenceNumberFormatted = String.format("%1$" + REFERENCE_NUMBER_LENGTH + "s", referenceNumber).replace(' ', '0');
            String reference = referenceYear + companyCode + referenceNumberFormatted;

            AccountingSlipDto accountingSlipDto = new AccountingSlipDto(
                    accountingSlipId,
                    companyCode,
                    "3N",
                    DateTimeUtil.getFormattedDateTime().toLocalDate(),
                    postingDate,
                    reference,
                    headertext,
                    "EUR",
                    rows
            );

            AccountingSlip createdSlip = createAccountingAndRows(accountingSlipDto);
            accountingSlipDtos.add(getAccountingSlipDtoWithRows(createdSlip));

            AccountingExportDataDto accountingExportDataDto = accountingExportDataService.createAccountingExportDataDto(accountingSlipDto);
            accountingExportService.exportAccountingData(accountingExportDataDto);
        }

        accountingsForDate.getValue().forEach(orderId -> orderService.markAsAccounted(orderId));

        return accountingSlipDtos;
    }

    public Map<String, List<OrderItemAccountingDto>> getSummedOrderItemAccountingsForDate(Map.Entry<LocalDate, List<String>> accountingsForDate) {
        List<OrderItemAccountingDto> accountingItemsForDate = new ArrayList<>();

        for (String id : accountingsForDate.getValue()) {
            List<OrderItemAccounting> list = orderItemAccountingService.getOrderItemAccountings(id);
            list.forEach(itemAccounting -> accountingItemsForDate.add(new OrderItemAccountingTransformer().transformToDto(itemAccounting)));
        }

        Map<String, List<OrderItemAccountingDto>> accountingsForCompanyCode = groupByCompanyCode(accountingItemsForDate);

        Map<String, List<OrderItemAccountingDto>> summedAccountingsForCompanyCode = new HashMap<>();
        for (Map.Entry<String, List<OrderItemAccountingDto>> entry : accountingsForCompanyCode.entrySet()) {
            // Sum prices per posting (same key info)
            Collection<OrderItemAccountingDto> values = entry.getValue().stream()
                    .collect(Collectors.toMap(OrderItemAccountingDto::createKey, Function.identity(), (OrderItemAccountingDto::merge)))
                    .values();

            summedAccountingsForCompanyCode.put(entry.getKey(), new ArrayList<>(values));
        }

        return summedAccountingsForCompanyCode;
    }

    private List<AccountingSlipRowDto> createAccountingSlipRowDtos(List<OrderItemAccountingDto> summedItemAccountings, String accountingSlipId,
                                                                   String lineText) {
        List<AccountingSlipRowDto> rows = new ArrayList<>();
        int rowNumber = 1;

        for (OrderItemAccountingDto summedItemAccounting : summedItemAccountings) {
            String accountingSlipRowId = UUIDGenerator.generateType3UUIDString(accountingSlipId, Integer.toString(rowNumber));

            AccountingSlipRowDto accountingSlipRowDto = AccountingSlipRowDto.builder()
                    .accountingSlipRowId(accountingSlipRowId)
                    .accountingSlipId(accountingSlipId)
                    .taxCode(summedItemAccounting.getVatCode())
                    .amountInDocumentCurrency(formatSum(summedItemAccounting.getPriceGrossAsDouble()))
                    .baseAmount(formatSum(summedItemAccounting.getPriceNetAsDouble()))
                    .vatAmount(formatSum(summedItemAccounting.getPriceVatAsDouble()))
                    .lineText(lineText)
                    .glAccount(summedItemAccounting.getMainLedgerAccount())
                    .profitCenter(summedItemAccounting.getProfitCenter())
                    .orderItemNumber(summedItemAccounting.getInternalOrder())
                    .wbsElement(summedItemAccounting.getProject())
                    .functionalArea(summedItemAccounting.getOperationArea())
                    .balanceProfitCenter(summedItemAccounting.getBalanceProfitCenter())
                    .build();

            rows.add(accountingSlipRowDto);
            rowNumber++;
        }

        return rows;
    }

    private Map<String, List<OrderItemAccountingDto>> groupByCompanyCode(List<OrderItemAccountingDto> accountingItemsForDate) {
        Map<String, List<OrderItemAccountingDto>> groupedAccountings = new HashMap<>();

        for (OrderItemAccountingDto orderItemAccountingDto : accountingItemsForDate) {
            String companyCode = orderItemAccountingDto.getCompanyCode();
            List<OrderItemAccountingDto> accountingList = groupedAccountings.get(companyCode);

            if (accountingList == null) {
                ArrayList<OrderItemAccountingDto> accountings = new ArrayList<>();
                accountings.add(orderItemAccountingDto);
                groupedAccountings.put(companyCode, accountings);
            } else {
                accountingList.add(orderItemAccountingDto);
            }
        }
        return groupedAccountings;
    }

    private AccountingSlip createAccountingAndRows(AccountingSlipDto accountingSlipDto) {
        AccountingSlip accountingSlip = accountingSlipRepository.save(new AccountingSlipTransformer().transformToEntity(accountingSlipDto));

        List<AccountingSlipRowDto> rows = accountingSlipDto.getRows();
        rows.forEach(row -> accountingSlipRowRepository.save(new AccountingSlipRowTransformer().transformToEntity(row)));

        log.debug("created new accounting slip, accountingId: " + accountingSlip.getAccountingSlipId());
        return accountingSlip;
    }

    private String formatSum(Double sum) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        decimalFormat.setNegativePrefix("-");

        return decimalFormat.format(-sum).replace(".", ",");
    }

}
