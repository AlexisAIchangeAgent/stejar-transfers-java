package md.stejar.transfers.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** A transfer to price. The amount is in the currency of the transfer. */
public record FeeRequest(BigDecimal amount, Currency currency, TransferType type, CustomerSegment customerSegment) {

    public FeeRequest {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(customerSegment, "customerSegment");
    }
}
