package md.stejar.transfers.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import md.stejar.transfers.domain.Currency;
import md.stejar.transfers.domain.CustomerSegment;
import md.stejar.transfers.domain.FeeRequest;
import md.stejar.transfers.domain.TransferType;

/** Body of POST /fees. */
public record FeeRequestBody(
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "MDL|EUR") String currency,
        @NotNull @Pattern(regexp = "standard|instant") String type,
        @NotNull @Pattern(regexp = "retail|premium") @JsonProperty("customer_segment") String customerSegment) {

    FeeRequest toDomain() {
        return new FeeRequest(
                amount,
                Currency.valueOf(currency),
                TransferType.fromCode(type),
                CustomerSegment.fromCode(customerSegment));
    }
}
