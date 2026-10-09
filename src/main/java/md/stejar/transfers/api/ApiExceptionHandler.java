package md.stejar.transfers.api;

import md.stejar.transfers.clients.ExchangeRateException;
import md.stejar.transfers.domain.UnsupportedTransferException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps errors to HTTP responses with a body {"error": "..."}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final int UNPROCESSABLE = 422;
    private static final int BAD_GATEWAY = 502;

    @ExceptionHandler(UnsupportedTransferException.class)
    public ResponseEntity<ErrorBody> unsupportedTransfer(UnsupportedTransferException e) {
        return ResponseEntity.status(UNPROCESSABLE).body(new ErrorBody(e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorBody> invalidRequest(Exception e) {
        return ResponseEntity.status(UNPROCESSABLE).body(new ErrorBody("invalid request"));
    }

    @ExceptionHandler(ExchangeRateException.class)
    public ResponseEntity<ErrorBody> rateUnavailable(ExchangeRateException e) {
        return ResponseEntity.status(BAD_GATEWAY).body(new ErrorBody("exchange rate unavailable"));
    }
}
