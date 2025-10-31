package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.accounting.*;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipRowTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemAccountingTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.RefundItemAccountingTransformer;
import fi.hel.verkkokauppa.order.constants.AccountingRowTypeEnum;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.*;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingSlipRepository;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingSlipRowRepository;
import fi.hel.verkkokauppa.common.util.ServiceUtils;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.refund.RefundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountingSlipService {

    private Logger log = LoggerFactory.getLogger(AccountingSlipService.class);

    public static final int REFERENCE_NUMBER_LENGTH = 3;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private OrderAccountingService orderAccountingService;

    @Autowired
    private RefundAccountingService refundAccountingService;

    @Autowired
    private OrderItemAccountingService orderItemAccountingService;

    @Autowired
    private RefundItemAccountingService refundItemAccountingService;

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
        List<OrderAccounting> ordersToAccount = accountingSearchService.findNotAccountedOrders();
        List<Refund> refundsToAccount = accountingSearchService.findNotAccountedRefunds();
        Map<LocalDate, List<String>> orderAccountingIdsByDate = groupOrderAccountingsByDate(ordersToAccount);
        Map<LocalDate, List<String>> refundAccountingIdsByDate = groupRefundAccountingsByDate(refundsToAccount);

        Set<LocalDate> slipDates = ServiceUtils.combineKeySets(
                orderAccountingIdsByDate.keySet(),
                refundAccountingIdsByDate.keySet(),
                LocalDate.class
        );

        // not handling current date
        if ((orderAccountingIdsByDate == null || orderAccountingIdsByDate.isEmpty())
                && (refundAccountingIdsByDate == null || refundAccountingIdsByDate.isEmpty())) {
            log.info("no orders or refunds to account");
            return new ArrayList<>();
        }

        return createAccountingSlips(slipDates, orderAccountingIdsByDate, refundAccountingIdsByDate);
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

    public List<AccountingSlipDto> createAccountingSlips(
            Set<LocalDate> slipDates,
            Map<LocalDate, List<String>> orderAccountingIdsByDate,
            Map<LocalDate, List<String>> refundAccountingIdsByDate
    ) {
        List<AccountingSlipDto> accountingSlips = new ArrayList<>();

        for (LocalDate date : slipDates) {
            List<AccountingSlipDto> accountingSlipDtos = createAccountingSlipForDate(date, orderAccountingIdsByDate.get(date), refundAccountingIdsByDate.get(date));
            accountingSlips.addAll(accountingSlipDtos);
        }
        return accountingSlips;
    }

    public Map<LocalDate, List<String>> groupOrderAccountingsByDate(List<OrderAccounting> orderAccountings) {
        Map<LocalDate, List<String>> map = new HashMap<>();

        for (OrderAccounting orderAccounting : orderAccountings) {
            LocalDate createdAt = DateTimeUtil.toFinnishDate(orderAccounting.getCreatedAt());
            LocalDate now = DateTimeUtil.toFinnishDate(LocalDateTime.now());

            if (createdAt.isBefore(now)) {
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

    public Map<LocalDate, List<String>> groupRefundAccountingsByDate(List<Refund> refundsToAccount) {
        Map<LocalDate, List<String>> map = new HashMap<>();

        List<String> refundIds = refundsToAccount.stream()
                .map(Refund::getRefundId)
                .collect(Collectors.toList());

        List<RefundAccounting> refundAccountings = refundAccountingService.getRefundAccountings(refundIds);

        for (RefundAccounting refundAccounting : refundAccountings) {
            LocalDate createdAt = DateTimeUtil.toFinnishDate(refundAccounting.getCreatedAt());
            LocalDate now = DateTimeUtil.toFinnishDate(LocalDateTime.now());

            if (createdAt.isBefore(now)) {
                List<String> accountingsForDate = map.get(createdAt);

                if (accountingsForDate == null) {
                    ArrayList<String> accountings = new ArrayList<>();
                    accountings.add(refundAccounting.getRefundId());
                    map.put(createdAt, accountings);
                } else {
                    accountingsForDate.add(refundAccounting.getRefundId());
                }
            }
        }

        return map;
    }

    private List<AccountingSlipDto> createAccountingSlipForDate(
            LocalDate postingDate,
            List<String> orderAccountingsForDate,
            List<String> refundAccountingsForDate) {
        List<AccountingSlipDto> accountingSlipDtos = new ArrayList<>();

        int referenceNumber = postingDate.getDayOfYear();

        // TODO: collect also orderids to summed accountings. Can order have items in multiple accountingSlips?
        Map<String, List<OrderItemAccountingDto>> summedOrderItemAccountings = getSummedOrderItemAccountingsForDate(orderAccountingsForDate);
        Map<String, List<RefundItemAccountingDto>> summedRefundItemAccountings = getSummedRefundItemAccountingsForDate(refundAccountingsForDate);

        Set<String> companyCodes = ServiceUtils.combineKeySets(
                summedOrderItemAccountings.keySet(),
                summedRefundItemAccountings.keySet(),
                String.class
        );

        for (String companyCode : companyCodes) {
            List<OrderItemAccountingDto> summedOrderItemAccountingsForCompanyCode = summedOrderItemAccountings.get(companyCode);
            List<RefundItemAccountingDto> summedRefundItemAccountingsForCompanyCode = summedRefundItemAccountings.get(companyCode);

            // TODO: if one accounting is missed and done later we overwrite earlier accounting export data with just one accounting row. How to make this unique? postingDate plus counter?
            String accountingSlipId = UUIDGenerator.generateType3UUIDString(postingDate.toString(), companyCode);

            String headerTextDate = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(postingDate);
            String headertext = "Verkkokauppa " + headerTextDate;

            // order each list by balanceProfitCenter
            Map<String, List<OrderItemAccountingDto>> orderAccountingsByBalanceProfitCenter;
            orderAccountingsByBalanceProfitCenter = groupOrderItemAccountingsByBalanceProfitCenter(summedOrderItemAccountingsForCompanyCode);
            Map<String, List<RefundItemAccountingDto>> refundAccountingsByBalanceProfitCenter;
            refundAccountingsByBalanceProfitCenter = groupRefundItemAccountingsByBalanceProfitCenter(summedRefundItemAccountingsForCompanyCode);

            // build set of all balanceProfitCenters
            Set<String> balanceProfitCenters = ServiceUtils.combineKeySets(
                    orderAccountingsByBalanceProfitCenter.keySet(),
                    refundAccountingsByBalanceProfitCenter.keySet(),
                    String.class
            );

            // collect all rows by balance profit center
            List<AccountingSlipRowDto> rows = new ArrayList<>();
            // rowCountOffset used to generate row id's for accounting slip rows
            // that get saved to db
            int rowCountOffset = 0;
            for (String balanceProfitCenter : balanceProfitCenters) {
                List<OrderItemAccountingDto> orderRows = orderAccountingsByBalanceProfitCenter.get(balanceProfitCenter);
                List<RefundItemAccountingDto> refundRows = refundAccountingsByBalanceProfitCenter.get(balanceProfitCenter);

                List<AccountingSlipRowDto> originalOrderRows = createAccountingSlipRowDtosForOrders(
                        orderRows,
                        accountingSlipId,
                        headertext,
                        rowCountOffset
                );
                rowCountOffset += originalOrderRows.size();

                List<AccountingSlipRowDto> originalRefundRows = createAccountingSlipRowDtosForRefunds(
                        refundRows,
                        accountingSlipId,
                        headertext,
                        rowCountOffset
                );
                rowCountOffset += originalRefundRows.size();

                List<AccountingSlipRowDto> separatedRows = new ArrayList<>();
                // separate VAT rows
                if (orderRows != null) {
                    separatedRows = accountingExportDataService.separateVatRows(originalOrderRows, separatedRows);
                }
                if (refundRows != null) {
                    separatedRows = accountingExportDataService.separateVatRows(originalRefundRows, separatedRows);
                }
                // add Income Entry rows
                if (orderRows != null) {
                    accountingExportDataService.addOrderIncomeEntryRow(originalOrderRows, separatedRows, headertext);
                }
                if (refundRows != null) {
                    accountingExportDataService.addRefundIncomeEntryRow(originalRefundRows, separatedRows, headertext);
                }
                rows.addAll(separatedRows);
            }

            String referenceYear = DateTimeFormatter.ofPattern("yy").format(postingDate);
            String referenceNumberFormatted = String.format("%1$" + REFERENCE_NUMBER_LENGTH + "s", referenceNumber).replace(' ', '0');
            String reference = referenceYear + companyCode + referenceNumberFormatted;

            // TODO: add order ids to slip dto and db row
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
            // TODO: add accountingSlipID to orderItemAccountings and refundItemAccountings if feasible.
        }

        if (orderAccountingsForDate != null) {
            orderAccountingsForDate.forEach(orderId -> orderService.markAsAccounted(orderId));
            orderAccountingsForDate.forEach(orderId -> orderAccountingService.markAsAccounted(orderId));
        }
        if (refundAccountingsForDate != null) {
            refundAccountingsForDate.forEach(refundId -> refundService.markAsAccounted(refundId));
        }

        return accountingSlipDtos;
    }

    public Map<String, List<OrderItemAccountingDto>> getSummedOrderItemAccountingsForDate(List<String> accountingsForDate) {
        List<OrderItemAccountingDto> accountingItemsForDate = new ArrayList<>();
        if (accountingsForDate == null) {
            return new HashMap<>();
        }

        for (String id : accountingsForDate) {
            List<OrderItemAccounting> list = orderItemAccountingService.getOrderItemAccountings(id);
            list.forEach(itemAccounting -> accountingItemsForDate.add(new OrderItemAccountingTransformer().transformToDto(itemAccounting)));
        }

        Map<String, List<OrderItemAccountingDto>> accountingsForCompanyCode = groupOrdersByCompanyCode(accountingItemsForDate);

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

    public Map<String, List<RefundItemAccountingDto>> getSummedRefundItemAccountingsForDate(List<String> accountingsForDate) {
        List<RefundItemAccountingDto> accountingItemsForDate = new ArrayList<>();
        if (accountingsForDate == null) {
            return new HashMap<>();
        }

        for (String refundAccountingId : accountingsForDate) {
            List<RefundItemAccounting> list = refundItemAccountingService.getRefundItemAccountings(refundAccountingId);
            list.forEach(itemAccounting -> accountingItemsForDate.add(new RefundItemAccountingTransformer().transformToDto(itemAccounting)));
        }

        Map<String, List<RefundItemAccountingDto>> accountingsForCompanyCode = groupRefundsByCompanyCode(accountingItemsForDate);

        Map<String, List<RefundItemAccountingDto>> summedAccountingsForCompanyCode = new HashMap<>();
        for (Map.Entry<String, List<RefundItemAccountingDto>> entry : accountingsForCompanyCode.entrySet()) {
            // Sum prices per posting (same key info)
            Collection<RefundItemAccountingDto> values = entry.getValue().stream()
                    .collect(Collectors.toMap(RefundItemAccountingDto::createKey, Function.identity(), (RefundItemAccountingDto::merge)))
                    .values();

            summedAccountingsForCompanyCode.put(entry.getKey(), new ArrayList<>(values));
        }

        return summedAccountingsForCompanyCode;
    }

    private List<AccountingSlipRowDto> createAccountingSlipRowDtosForOrders(List<OrderItemAccountingDto> summedItemAccountings, String accountingSlipId,
                                                                            String lineText, int rowCountOffset) {
        List<AccountingSlipRowDto> rows = new ArrayList<>();
        if (summedItemAccountings == null) {
            return rows;
        }

        int rowNumber = 1 + rowCountOffset;

        for (OrderItemAccountingDto summedItemAccounting : summedItemAccountings) {
            String accountingSlipRowId = UUIDGenerator.generateType3UUIDString(accountingSlipId, Integer.toString(rowNumber));

            AccountingSlipRowDto accountingSlipRowDto = AccountingSlipRowDto.builder()
                    .accountingSlipRowId(accountingSlipRowId)
                    .accountingSlipId(accountingSlipId)
                    .taxCode(summedItemAccounting.getVatCode())
                    .amountInDocumentCurrency(formatOrderSum(summedItemAccounting.getPriceGrossAsDouble()))
                    .baseAmount(formatOrderSum(summedItemAccounting.getPriceNetAsDouble()))
                    .vatAmount(formatOrderSum(summedItemAccounting.getPriceVatAsDouble()))
                    .lineText(lineText)
                    .glAccount(summedItemAccounting.getMainLedgerAccount())
                    .profitCenter(summedItemAccounting.getProfitCenter())
                    .orderItemNumber(summedItemAccounting.getInternalOrder())
                    .wbsElement(summedItemAccounting.getProject())
                    .functionalArea(summedItemAccounting.getOperationArea())
                    .balanceProfitCenter(summedItemAccounting.getBalanceProfitCenter())
                    .rowType(AccountingRowTypeEnum.ORDER)
                    .build();

            rows.add(accountingSlipRowDto);
            rowNumber++;
        }

        return rows;
    }

    private List<AccountingSlipRowDto> createAccountingSlipRowDtosForRefunds(List<RefundItemAccountingDto> summedItemAccountings, String accountingSlipId,
                                                                             String lineText, int rowCountOffset) {
        List<AccountingSlipRowDto> rows = new ArrayList<>();
        if (summedItemAccountings == null) {
            return rows;
        }

        int rowNumber = 1 + rowCountOffset;

        for (RefundItemAccountingDto summedItemAccounting : summedItemAccountings) {
            String accountingSlipRowId = UUIDGenerator.generateType3UUIDString(accountingSlipId, Integer.toString(rowNumber));

            AccountingSlipRowDto accountingSlipRowDto = AccountingSlipRowDto.builder()
                    .accountingSlipRowId(accountingSlipRowId)
                    .accountingSlipId(accountingSlipId)
                    .taxCode(summedItemAccounting.getVatCode())
                    .amountInDocumentCurrency(formatRefundSum(summedItemAccounting.getPriceGrossAsDouble()))
                    .baseAmount(formatRefundSum(summedItemAccounting.getPriceNetAsDouble()))
                    .vatAmount(formatRefundSum(summedItemAccounting.getPriceVatAsDouble()))
                    .lineText(lineText)
                    .glAccount(summedItemAccounting.getMainLedgerAccount())
                    .profitCenter(summedItemAccounting.getProfitCenter())
                    .orderItemNumber(summedItemAccounting.getInternalOrder())
                    .wbsElement(summedItemAccounting.getProject())
                    .functionalArea(summedItemAccounting.getOperationArea())
                    .balanceProfitCenter(summedItemAccounting.getBalanceProfitCenter())
                    .rowType(AccountingRowTypeEnum.REFUND)
                    .build();

            rows.add(accountingSlipRowDto);
            rowNumber++;
        }

        return rows;
    }

    private Map<String, List<OrderItemAccountingDto>> groupOrdersByCompanyCode(List<OrderItemAccountingDto> accountingItemsForDate) {
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

    private Map<String, List<RefundItemAccountingDto>> groupRefundsByCompanyCode(List<RefundItemAccountingDto> accountingItemsForDate) {
        Map<String, List<RefundItemAccountingDto>> groupedAccountings = new HashMap<>();

        for (RefundItemAccountingDto refundItemAccountingDto : accountingItemsForDate) {
            String companyCode = refundItemAccountingDto.getCompanyCode();
            List<RefundItemAccountingDto> accountingList = groupedAccountings.get(companyCode);

            if (accountingList == null) {
                ArrayList<RefundItemAccountingDto> accountings = new ArrayList<>();
                accountings.add(refundItemAccountingDto);
                groupedAccountings.put(companyCode, accountings);
            } else {
                accountingList.add(refundItemAccountingDto);
            }
        }
        return groupedAccountings;
    }

    private Map<String, List<OrderItemAccountingDto>> groupOrderItemAccountingsByBalanceProfitCenter(List<OrderItemAccountingDto> accountingItemsForDate) {
        Map<String, List<OrderItemAccountingDto>> groupedAccountings = new HashMap<>();
        if (accountingItemsForDate == null) {
            return groupedAccountings;
        }

        for (OrderItemAccountingDto orderItemAccountingDto : accountingItemsForDate) {
            String balanceProfitCenter = orderItemAccountingDto.getBalanceProfitCenter();
            List<OrderItemAccountingDto> accountingList = groupedAccountings.get(balanceProfitCenter);

            if (accountingList == null) {
                ArrayList<OrderItemAccountingDto> accountings = new ArrayList<>();
                accountings.add(orderItemAccountingDto);
                groupedAccountings.put(balanceProfitCenter, accountings);
            } else {
                accountingList.add(orderItemAccountingDto);
            }
        }
        return groupedAccountings;
    }

    private Map<String, List<RefundItemAccountingDto>> groupRefundItemAccountingsByBalanceProfitCenter(List<RefundItemAccountingDto> accountingItemsForDate) {
        Map<String, List<RefundItemAccountingDto>> groupedAccountings = new HashMap<>();
        if (accountingItemsForDate == null) {
            return groupedAccountings;
        }

        for (RefundItemAccountingDto refundItemAccountingDto : accountingItemsForDate) {
            String balanceProfitCenter = refundItemAccountingDto.getBalanceProfitCenter();
            List<RefundItemAccountingDto> accountingList = groupedAccountings.get(balanceProfitCenter);

            if (accountingList == null) {
                ArrayList<RefundItemAccountingDto> accountings = new ArrayList<>();
                accountings.add(refundItemAccountingDto);
                groupedAccountings.put(balanceProfitCenter, accountings);
            } else {
                accountingList.add(refundItemAccountingDto);
            }
        }
        return groupedAccountings;
    }

    public Map<String, List<AccountingSlipRowDto>> groupAccountingSlipRowsByBalanceProfitCenter(List<AccountingSlipRowDto> rows) {
        Map<String, List<AccountingSlipRowDto>> groupedAccountings = new HashMap<>();

        for (AccountingSlipRowDto row : rows) {
            String balanceProfitCenter = row.getBalanceProfitCenter();
            List<AccountingSlipRowDto> accountingList = groupedAccountings.get(balanceProfitCenter);

            if (accountingList == null) {
                ArrayList<AccountingSlipRowDto> accountings = new ArrayList<>();
                accountings.add(row);
                groupedAccountings.put(balanceProfitCenter, accountings);
            } else {
                accountingList.add(row);
            }
        }
        return groupedAccountings;
    }

    private AccountingSlip createAccountingAndRows(AccountingSlipDto accountingSlipDto) {
        AccountingSlip accountingSlip = accountingSlipRepository.save(new AccountingSlipTransformer().transformToEntity(accountingSlipDto));

        List<AccountingSlipRowDto> rows = accountingSlipDto.getRows();

        rows.forEach(row -> {
            // do not save vat or income entry rows
            if (row.getAccountingSlipRowId() != null) {
                accountingSlipRowRepository.save(new AccountingSlipRowTransformer().transformToEntity(row));
                // set amountInDocumentCurrency to base amount for sum rows (for XML)
                row.setAmountInDocumentCurrency(row.getBaseAmount());
                // set base amount for sum rows to null (for XML)
                row.setBaseAmount(null);
            }
        });

        log.debug("created new accounting slip, accountingId: " + accountingSlip.getAccountingSlipId());
        return accountingSlip;
    }

    private String formatOrderSum(Double sum) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        decimalFormat.setNegativePrefix("-");

        return decimalFormat.format(-sum).replace(".", ",");
    }

    private String formatRefundSum(Double sum) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        decimalFormat.setPositivePrefix("+");

        return decimalFormat.format(sum).replace(".", ",");
    }


}
