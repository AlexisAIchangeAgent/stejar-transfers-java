package md.stejar.transfers.clients;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A rate given by the exchange-rate provider: 1 unit of {@code from} is {@code rate} units of {@code to}. */
public record ExchangeRate(String from, String to, BigDecimal rate, LocalDate asOf) {}
