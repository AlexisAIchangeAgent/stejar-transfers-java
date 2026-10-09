package md.stejar.transfers.api;

import jakarta.validation.Valid;
import java.util.Map;
import md.stejar.transfers.domain.FeeCalculator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** HTTP routes of the fee service. */
@RestController
public class FeeController {

    private final FeeCalculator calculator;

    public FeeController(FeeCalculator calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/fees")
    public FeeResponseBody computeFee(@Valid @RequestBody FeeRequestBody body) {
        return FeeResponseBody.from(calculator.calculate(body.toDomain()));
    }
}
