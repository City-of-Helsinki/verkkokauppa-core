package fi.hel.verkkokauppa.common.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
@Slf4j
public class PriceConversionService {

    public BigInteger convertEuroStringToBigIntegerCents(String amount) {
        // split euro string to euros and cents
        String[] total = amount.split("\\.");

        // bigInt with euros from string
        BigInteger bigIntAmount = new BigInteger(total[0] + "00");

        // add cents
        BigInteger cents = new BigInteger(total[0]);
        bigIntAmount = bigIntAmount.add(cents);

        return bigIntAmount;
    }

    public String convertBigIntegerCentsToEuroString(BigInteger amount) {
        // Add euros (divide), decimal point and cents (remainder) to make amount string in euros
        return amount.divide(new BigInteger("100")).toString() + "." + amount.remainder(new BigInteger("100")).toString();
    }
}
