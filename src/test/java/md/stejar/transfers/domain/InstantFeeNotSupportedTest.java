package md.stejar.transfers.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import md.stejar.transfers.clients.FixedRateClient;
import org.junit.jupiter.api.Test;

class InstantFeeNotSupportedTest {

    @Test
    void instantTransferIsRejectedByTheCalculator() {
        FeeCalculator calculator = new FeeCalculator(new FixedRateClient());
        FeeRequest request =
                new FeeRequest(new BigDecimal("2000.00"), Currency.MDL, TransferType.INSTANT, CustomerSegment.RETAIL);

        assertThatThrownBy(() -> calculator.calculate(request))
                .isInstanceOf(UnsupportedTransferException.class)
                .hasMessage("instant transfers not supported yet");
    }
}
