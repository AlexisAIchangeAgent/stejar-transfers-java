package md.stejar.transfers.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Calls the exchange-rate API over HTTP. */
public class HttpExchangeRateClient implements ExchangeRateClient {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String baseUrl;
    private final String apiKey;
    private final Duration timeout;
    private final HttpClient httpClient;

    public HttpExchangeRateClient(String baseUrl, String apiKey, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public ExchangeRate getRate(String from, String to) {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create(baseUrl + "/rates?from=" + encode(from) + "&to=" + encode(to)))
                        .timeout(timeout)
                        .header("Accept", "application/json")
                        .GET();
        if (apiKey != null && !apiKey.isBlank()) {
            request.header("X-Api-Key", apiKey);
        }
        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ExchangeRateException(
                        "cannot get rate " + from + "/" + to + ": HTTP " + response.statusCode());
            }
            RateBody body = MAPPER.readValue(response.body(), RateBody.class);
            if (body.from() == null || body.to() == null || body.rate() == null || body.asOf() == null) {
                throw new ExchangeRateException("cannot get rate " + from + "/" + to + ": incomplete response");
            }
            return new ExchangeRate(body.from(), body.to(), body.rate(), LocalDate.parse(body.asOf()));
        } catch (IOException | DateTimeParseException e) {
            throw new ExchangeRateException("cannot get rate " + from + "/" + to, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateException("cannot get rate " + from + "/" + to, e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record RateBody(String from, String to, BigDecimal rate, @JsonProperty("as_of") String asOf) {}
}
