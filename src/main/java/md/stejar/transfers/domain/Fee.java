package md.stejar.transfers.domain;

import java.math.BigDecimal;

/** A computed fee, always in MDL, with the name of the rule applied. */
public record Fee(BigDecimal amount, Currency currency, String rule) {

    public static Fee mdl(BigDecimal amount, String rule) {
        return new Fee(amount, Currency.MDL, rule);
    }
}
