package md.stejar.transfers;

import java.time.Duration;
import md.stejar.transfers.clients.ExchangeRateClient;
import md.stejar.transfers.clients.FixedRateClient;
import md.stejar.transfers.clients.HttpExchangeRateClient;
import md.stejar.transfers.domain.FeeCalculator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the domain to its clients. The domain itself has no Spring and no HTTP. */
@Configuration
@EnableConfigurationProperties(RatesProperties.class)
public class AppConfiguration {

    /** HTTP client when RATES_BASE_URL is set, otherwise the fixed-rate stub. */
    @Bean
    public ExchangeRateClient exchangeRateClient(RatesProperties properties) {
        if (properties.hasBaseUrl()) {
            return new HttpExchangeRateClient(
                    properties.baseUrl(),
                    properties.apiKey(),
                    Duration.ofSeconds(properties.timeoutSeconds()));
        }
        return new FixedRateClient();
    }

    @Bean
    public FeeCalculator feeCalculator(ExchangeRateClient exchangeRateClient) {
        return new FeeCalculator(exchangeRateClient);
    }
}
