package fi.hel.verkkokauppa.order.service.accounting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.jcraft.jsch.*;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.IterableUtils;
import fi.hel.verkkokauppa.order.api.data.accounting.*;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingExportDataTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipTransformer;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingExportDataRepository;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountingExportDataService {

    public static final String VAT_LINE_TEXT = "ALV:n osuus";
    public static final String VAT_LINE_GL_ACCOUNT = "263200";
    public static final String INCOME_ENTRY_GL_ACCOUNT = "171810";
    public static final String INCOME_ENTRY_PROFIT_CENTER = "2923000";

    @Autowired
    private AccountingExportDataRepository exportDataRepository;

    private Logger log = LoggerFactory.getLogger(AccountingExportDataService.class);

    public AccountingExportDataDto createAccountingExportDataDto(AccountingSlipDto accountingSlip) {
        List<AccountingSlipRowDto> originalRows = accountingSlip.getRows();

        List<AccountingSlipRowDto> separatedRows = separateVatRows(originalRows);
        addIncomeEntryRow(originalRows, separatedRows, accountingSlip.getHeaderText());
        accountingSlip.setRows(separatedRows);

        String xml;
        try {
            xml = generateAccountingExportXML(accountingSlip);
        } catch (JsonProcessingException jpe) {
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-accounting-export-data-xml", "failed to create accounting export data xml")
            );
        }

        log.debug("generated accounting export data xml successfully");

        String postingDate = accountingSlip.getPostingDate();
        LocalDate postingDateFormatted = LocalDate.parse(postingDate, DateTimeFormatter.BASIC_ISO_DATE);

        AccountingExportDataDto exportDataDto = new AccountingExportDataDto(
                accountingSlip.getAccountingSlipId(),
                postingDateFormatted.toString(),
                xml
        );

        createAccountingExportData(exportDataDto);
        return exportDataDto;
    }

    private List<AccountingSlipRowDto> separateVatRows(List<AccountingSlipRowDto> originalRows) {
        List<AccountingSlipRowDto> separatedRows = new ArrayList<>();

        for (AccountingSlipRowDto originalRow : originalRows) {
            String baseAmount = originalRow.getBaseAmount();

            AccountingSlipRowDto row = new AccountingSlipRowDto(originalRow);
            row.setAmountInDocumentCurrency(baseAmount);
            row.setBaseAmount(null);

            AccountingSlipRowDto vatRow = AccountingSlipRowDto.builder()
                    .taxCode(originalRow.getTaxCode())
                    .amountInDocumentCurrency(originalRow.getVatAmount())
                    .baseAmount(baseAmount)
                    .lineText(VAT_LINE_TEXT)
                    .build();

            separatedRows.add(row);
            separatedRows.add(vatRow);
        }

        return separatedRows;
    }

    private void addIncomeEntryRow(List<AccountingSlipRowDto> originalRows, List<AccountingSlipRowDto> rows, String lineText) {
        double sum = originalRows.stream().mapToDouble(AccountingSlipRowDto::getAmountInDocumentCurrencyAsDouble).sum();

        AccountingSlipRowDto incomeEntryRow = AccountingSlipRowDto.builder()
                .amountInDocumentCurrency(formatIncomeEntrySum(sum))
                .lineText(lineText)
                .glAccount(INCOME_ENTRY_GL_ACCOUNT)
                .profitCenter(INCOME_ENTRY_PROFIT_CENTER)
                .build();

        rows.add(incomeEntryRow);
    }

    public String generateAccountingExportXML(AccountingSlipDto accountingSlip) throws JsonProcessingException {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        mapper.addMixIn(AccountingSlipDto.class, AccountingSlipMixInDto.class);
        mapper.addMixIn(AccountingSlipRowDto.class, AccountingSlipRowMixInDto.class);

        AccountingSlipWrapperDto wrapper = new AccountingSlipWrapperDto(accountingSlip);

        return mapper.writeValueAsString(wrapper);
    }

    public AccountingExportData createAccountingExportData(AccountingExportDataDto dto) {
        AccountingExportData entity = exportDataRepository.save(new AccountingExportDataTransformer().transformToEntity(dto));
        log.debug("created new accounting export data, timestamp: " + entity.getTimestamp());

        return entity;
    }

    public AccountingExportData getAccountingExportData(String accountingSlipId) {
        Optional<AccountingExportData> exportData = exportDataRepository.findById(accountingSlipId);

        if (exportData.isPresent()) {
            return exportData.get();
        }

        throw new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("accounting-export-data-not-found-from-backend",
                        "accounting export data with id [" + accountingSlipId + "]  not found from backend")
        );
    }

    public List<AccountingExportData> getAccountingExportDataByTimestamp(String timestamp) {
        List<AccountingExportData> exportData = exportDataRepository.findAllByTimestamp(timestamp);

        if (exportData != null && !exportData.isEmpty()) {
            return exportData;
        }

        throw new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("accounting-export-data-not-found-from-backend",
                        "accounting export data with timestamp [" + timestamp + "]  not found from backend")
        );
    }

//    public List<AccountingExportData> getNotExportedAccountingExportData() {
//        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));
//
//        NativeSearchQuery query = new NativeSearchQueryBuilder()
//                .withQuery(qb)
//                .withPageable(PageRequest.of(0, 10000))
//                .build();
//
//        SearchHits<AccountingExportData> hits = operations.search(query, AccountingExportData.class);
//
//        SearchPage<AccountingExportData> searchHits = SearchHitSupport.searchPageFor(hits, query.getPageable());
//
//        final List<AccountingExportData> exportData = searchHits.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        if (exportData == null || exportData.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        return exportData;
//    }

    public List<String> getAccountingExportDataTimestamps() {
        List<AccountingExportData> exportDatas = IterableUtils.iterableToList(exportDataRepository.findAll());

        if (exportDatas == null || exportDatas.isEmpty()) {
            return new ArrayList<>();
        }

        return exportDatas.stream()
                .map(AccountingExportData::getTimestamp)
                .distinct()
                .collect(Collectors.toList());
    }

    private String formatIncomeEntrySum(Double sum) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");

        return "+" + decimalFormat.format(-sum).replace(".", ",");
    }

}
