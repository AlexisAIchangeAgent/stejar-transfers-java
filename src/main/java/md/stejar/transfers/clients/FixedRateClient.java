package md.stejar.transfers.clients;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Stub used in tests and local runs: always returns the same rate, no network. */
public record FixedRateClient(BigDecimal rate, LocalDate asOf) implements ExchangeRateClient {

    public static final BigDecimal DEFAULT_RATE = new BigDecimal("19.8765");
    public static final LocalDate DEFAULT_AS_OF = LocalDate.of(2026, 10, 19);

    public FixedRateClient() {
        this(DEFAULT_RATE, DEFAULT_AS_OF);
    }

    public FixedRateClient(BigDecimal rate) {
        this(rate, DEFAULT_AS_OF);
    }

    @Override
    public ExchangeRate getRate(String from, String to) {
        return new ExchangeRate(from, to, rate, asOf);
    }
}
