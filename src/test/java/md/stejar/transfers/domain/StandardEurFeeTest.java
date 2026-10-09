package md.stejar.transfers.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import md.stejar.transfers.clients.ExchangeRate;
import md.stejar.transfers.clients.ExchangeRateClient;
import md.stejar.transfers.clients.FixedRateClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StandardEurFeeTest {

    private final FeeCalculator calculator = new FeeCalculator(new FixedRateClient(new BigDecimal("20.0000")));

    private static FeeRequest eurRequest(String amount) {
        return new FeeRequest(new BigDecimal(amount), Currency.EUR, TransferType.STANDARD, CustomerSegment.RETAIL);
    }

    /** Test double that records the pairs of currencies it is asked for. */
    private static final class RecordingRateClient implements ExchangeRateClient {

        private final List<String> calls = new ArrayList<>();

        @Override
        public ExchangeRate getRate(String from, String to) {
            calls.add(from + "/" + to);
            return new ExchangeRate(from, to, new BigDecimal("20.0000"), LocalDate.of(2026, 10, 19));
        }
    }

    @ParameterizedTest
    @CsvSource({"100.00, 10.00", "300.00, 30.00", "10.00, 5.00", "1000.00, 50.00"})
    void convertsToMdlThenAppliesTheMdlRule(String amountEur, String expectedFee) {
        Fee fee = calculator.calculate(eurRequest(amountEur));

        assertThat(fee.amount()).isEqualTo(new BigDecimal(expectedFee));
        assertThat(fee.currency()).isEqualTo(Currency.MDL);
        assertThat(fee.rule()).isEqualTo("standard-eur");
    }

    @Test
    void asksTheEurToMdlRate() {
        RecordingRateClient rates = new RecordingRateClient();

        new FeeCalculator(rates).calculate(eurRequest("100.00"));

        assertThat(rates.calls).containsExactly("EUR/MDL");
    }

    @Test
    void usesThePublishedRate() {
        FeeCalculator withPublishedRate = new FeeCalculator(new FixedRateClient());

        // EUR 100.00 at 19.8765 is MDL 1,987.65, and 0.5% of it is 9.93825
        assertThat(withPublishedRate.calculate(eurRequest("100.00")).amount()).isEqualTo(new BigDecimal("9.94"));
    }

    @Test
    void mdlTransferDoesNotAskForARate() {
        RecordingRateClient rates = new RecordingRateClient();

        new FeeCalculator(rates)
                .calculate(new FeeRequest(
                        new BigDecimal("100.00"), Currency.MDL, TransferType.STANDARD, CustomerSegment.RETAIL));

        assertThat(rates.calls).isEmpty();
    }
}
