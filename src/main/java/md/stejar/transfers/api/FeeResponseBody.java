package md.stejar.transfers.api;

import java.math.BigDecimal;
import md.stejar.transfers.domain.Fee;

/** Body of a 200 response of POST /fees. The fee is written as a JSON number with 2 decimals. */
public record FeeResponseBody(BigDecimal fee, String currency, String rule) {

    static FeeResponseBody from(Fee fee) {
        return new FeeResponseBody(fee.amount(), fee.currency().name(), fee.rule());
    }
}
