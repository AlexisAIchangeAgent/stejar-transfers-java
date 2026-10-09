package md.stejar.transfers.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.stream.Stream;
import md.stejar.transfers.clients.FixedRateClient;
import md.stejar.transfers.domain.FeeCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(FeeController.class)
class FeesEndpointTest {

    @TestConfiguration
    static class FixedRate {

        @Bean
        FeeCalculator feeCalculator() {
            return new FeeCalculator(new FixedRateClient(new BigDecimal("20.0000")));
        }
    }

    @Autowired
    private MockMvc mvc;

    private ResultActions postFees(String json) throws Exception {
        return mvc.perform(post("/fees").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String feeBody(String amount, String currency, String type, String segment) {
        return "{\"amount\": %s, \"currency\": \"%s\", \"type\": \"%s\", \"customer_segment\": \"%s\"}"
                .formatted(amount, currency, type, segment);
    }

    @Test
    void healthReturnsOk() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\": \"ok\"}", JsonCompareMode.STRICT));
    }

    @Test
    void postFeesReturnsTheStandardMdlFee() throws Exception {
        postFees(feeBody("2000.00", "MDL", "standard", "retail"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"fee\": 10.00, \"currency\": \"MDL\", \"rule\": \"standard-mdl\"}", JsonCompareMode.STRICT));
    }

    @Test
    void postFeesReturnsTheFeeAsAJsonNumberWithTwoDecimals() throws Exception {
        postFees(feeBody("2000", "MDL", "standard", "retail"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"fee\":10.00")));
    }

    @Test
    void postFeesReturnsTheStandardEurFeeInMdl() throws Exception {
        postFees(feeBody("300.00", "EUR", "standard", "premium"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"fee\": 30.00, \"currency\": \"MDL\", \"rule\": \"standard-eur\"}", JsonCompareMode.STRICT));
    }

    static Stream<String> invalidBodies() {
        return Stream.of(
                feeBody("100.00", "USD", "standard", "retail"),
                feeBody("0", "MDL", "standard", "retail"),
                feeBody("-5.00", "MDL", "standard", "retail"),
                feeBody("10.001", "MDL", "standard", "retail"),
                feeBody("100.00", "MDL", "express", "retail"),
                feeBody("100.00", "MDL", "standard", "vip"),
                "{\"currency\": \"MDL\", \"type\": \"standard\", \"customer_segment\": \"retail\"}");
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void postFeesRejectsAnInvalidBodyWith422(String json) throws Exception {
        postFees(json).andExpect(status().is(422));
    }

    @ParameterizedTest
    @CsvSource({"MDL, retail", "MDL, premium", "EUR, retail", "EUR, premium"})
    void postFeesReturns422ForAnInstantTransfer(String currency, String segment) throws Exception {
        postFees(feeBody("2000.00", currency, "instant", segment))
                .andExpect(status().is(422))
                .andExpect(content().json("{\"error\": \"instant transfers not supported yet\"}", JsonCompareMode.STRICT));
    }
}
