package md.stejar.transfers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the exchange-rate client. Bound from application.properties, which reads the
 * environment variables RATES_BASE_URL, RATES_API_KEY and RATES_TIMEOUT_SECONDS.
 */
@ConfigurationProperties(prefix = "rates")
public record RatesProperties(String baseUrl, String apiKey, @DefaultValue("5") int timeoutSeconds) {

    public boolean hasBaseUrl() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
