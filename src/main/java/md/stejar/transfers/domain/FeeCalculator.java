package md.stejar.transfers.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import md.stejar.transfers.clients.ExchangeRateClient;

/** Fee calculation. The published tariff is in docs/fee-rules.md. */
public class FeeCalculator {

    static final BigDecimal STANDARD_RATE = new BigDecimal("0.005");
    static final BigDecimal STANDARD_MIN_FEE = new BigDecimal("5.00");
    static final BigDecimal STANDARD_MAX_FEE = new BigDecimal("50.00");
    private static final int CENTS = 2;

    private final ExchangeRateClient rates;

    public FeeCalculator(ExchangeRateClient rates) {
        this.rates = rates;
    }

    public Fee calculate(FeeRequest request) {
        if (request.type() == TransferType.INSTANT) {
            throw new UnsupportedTransferException("instant transfers not supported yet");
        }
        if (request.currency() == Currency.MDL) {
            return Fee.mdl(standardFeeMdl(request.amount()), "standard-mdl");
        }
        return Fee.mdl(standardFeeEur(request.amount()), "standard-eur");
    }

    /** Standard fee of an MDL amount: 0.5%, minimum MDL 5.00, maximum MDL 50.00. */
    static BigDecimal standardFeeMdl(BigDecimal amountMdl) {
        BigDecimal fee = amountMdl.multiply(STANDARD_RATE).setScale(CENTS, RoundingMode.HALF_UP);
        return clamp(fee);
    }

    private BigDecimal standardFeeEur(BigDecimal amountEur) {
        BigDecimal rate = rates.getRate(Currency.EUR.name(), Currency.MDL.name()).rate();
        BigDecimal amountMdl = amountEur.multiply(rate).setScale(CENTS, RoundingMode.DOWN);
        BigDecimal fee = amountMdl.multiply(STANDARD_RATE).setScale(CENTS, RoundingMode.HALF_EVEN);
        return clamp(fee);
    }

    private static BigDecimal clamp(BigDecimal fee) {
        return fee.max(STANDARD_MIN_FEE).min(STANDARD_MAX_FEE);
    }
}
