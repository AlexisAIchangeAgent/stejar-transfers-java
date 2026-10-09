package md.stejar.transfers.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import md.stejar.transfers.clients.FixedRateClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StandardMdlFeeTest {

    private final FeeCalculator calculator = new FeeCalculator(new FixedRateClient(new BigDecimal("20.0000")));

    private static FeeRequest mdlRequest(String amount, CustomerSegment segment) {
        return new FeeRequest(new BigDecimal(amount), Currency.MDL, TransferType.STANDARD, segment);
    }

    private static FeeRequest mdlRequest(String amount) {
        return mdlRequest(amount, CustomerSegment.RETAIL);
    }

    @ParameterizedTest
    @CsvSource({"2000.00, 10.00", "1234.00, 6.17", "9999.00, 50.00"})
    void isHalfAPercent(String amount, String expectedFee) {
        Fee fee = calculator.calculate(mdlRequest(amount));

        assertThat(fee.amount()).isEqualTo(new BigDecimal(expectedFee));
        assertThat(fee.currency()).isEqualTo(Currency.MDL);
        assertThat(fee.rule()).isEqualTo("standard-mdl");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "100.00", "999.99", "1000.00"})
    void hasAMinimumOf5(String amount) {
        assertThat(calculator.calculate(mdlRequest(amount)).amount()).isEqualTo(new BigDecimal("5.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10000.00", "10000.01", "250000.00"})
    void hasAMaximumOf50(String amount) {
        assertThat(calculator.calculate(mdlRequest(amount)).amount()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void roundsHalfUp() {
        // 0.5% of 1001.00 is 5.005
        assertThat(calculator.calculate(mdlRequest("1001.00")).amount()).isEqualTo(new BigDecimal("5.01"));
    }

    @Test
    void hasTwoDecimals() {
        assertThat(calculator.calculate(mdlRequest("2000.00")).amount().scale()).isEqualTo(2);
    }

    @Test
    void premiumFeeIsTheRetailFee() {
        Fee retail = calculator.calculate(mdlRequest("3000.00", CustomerSegment.RETAIL));
        Fee premium = calculator.calculate(mdlRequest("3000.00", CustomerSegment.PREMIUM));

        assertThat(premium).isEqualTo(retail);
    }
}
