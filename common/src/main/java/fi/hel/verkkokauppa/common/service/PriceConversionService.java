package fi.hel.verkkokauppa.common.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
@Slf4j
public class PriceConversionService {

    public Integer convertEuroStringToIntegerCents(String amount) {
        if (amount == null) {
            return 0;
        }
        // split euro string to euros and cents
        String[] total = amount.split("\\.");

        // Integer with euros from string
        Integer intAmount = Integer.parseInt(total[0] + "00");

        // add cents
        if( total.length > 1 ) {
            Integer cents = Integer.parseInt(total[1]);
            intAmount = intAmount + cents;
        }

        return intAmount;
    }

    public String convertIntegerCentsToEuroString(Integer amount) {
        if (amount == null) {
            return "0.00";
        }
        // Add euros (divide), decimal point and cents (remainder) to make amount string in euros
        int euros = (amount / 100);
        int cents = (amount % 100);

        return String.format("€%d,%02d", euros, cents);
    }
}
