package fi.hel.verkkokauppa.price.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceServiceTest {

    @Test
    public void testCalculateNetAndVat() {
        double grossValue = 100.0;
        double vatPercentage = 24.0;
        double[] expected = {80.65, 19.35}; // Expected net value and VAT value

        double[] result = PriceService.calculateNetAndVat(grossValue, vatPercentage);

        // Check if the calculated net value and VAT value match the expected values
        double netValue = result[0];
        assertEquals(expected[0], netValue, 0.01); // Using delta to allow for rounding errors
        double vatValue = result[1];
        assertEquals(expected[1], vatValue, 0.01);
    }

    @Test
    public void testCalculateNetAndVatNewVat() {
        double grossValue = 100.0;
        double vatPercentage = 25.5;
        double[] expected = {79.68, 20.32}; // Expected net value and VAT value

        double[] result = PriceService.calculateNetAndVat(grossValue, vatPercentage);

        // Check if the calculated net value and VAT value match the expected values
        double netValue = result[0];
        assertEquals(expected[0], netValue, 0.01); // Using delta to allow for rounding errors
        double vatValue = result[1];
        assertEquals(expected[1], vatValue, 0.01);
    }

    @Test
    public void testCalculateNetAndVatWithZeroGrossValue() {
        double grossValue = 0.0;
        double vatPercentage = 20.0;
        double[] expected = {0.0, 0.0}; // Expected net value and VAT value

        double[] result = PriceService.calculateNetAndVat(grossValue, vatPercentage);

        // Check if the calculated net value and VAT value match the expected values
        assertEquals(expected[0], result[0], 0.01); // Using delta to allow for rounding errors
        assertEquals(expected[1], result[1], 0.01);
    }

    @Test
    public void testCalculateNetAndVatWithZeroVATPercentage() {
        double grossValue = 120.0;
        double vatPercentage = 0.0;
        double[] expected = {120.0, 0.0}; // Expected net value and VAT value

        double[] result = PriceService.calculateNetAndVat(grossValue, vatPercentage);

        // Check if the calculated net value and VAT value match the expected values
        assertEquals(expected[0], result[0], 0.01); // Using delta to allow for rounding errors
        assertEquals(expected[1], result[1], 0.01);
    }
}