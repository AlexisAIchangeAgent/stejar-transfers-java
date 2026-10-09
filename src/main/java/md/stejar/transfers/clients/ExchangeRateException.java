package md.stejar.transfers.clients;

import java.io.Serial;

/** The exchange rate could not be obtained. */
public class ExchangeRateException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public ExchangeRateException(String message) {
        super(message);
    }

    public ExchangeRateException(String message, Throwable cause) {
        super(message, cause);
    }
}
