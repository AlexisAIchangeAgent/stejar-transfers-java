package md.stejar.transfers.domain;

import java.io.Serial;

/** The transfer cannot be priced by the current rules. */
public class UnsupportedTransferException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public UnsupportedTransferException(String message) {
        super(message);
    }
}
