package md.stejar.transfers;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import md.stejar.transfers.clients.ExchangeRate;
import md.stejar.transfers.clients.ExchangeRateClient;
import md.stejar.transfers.clients.FixedRateClient;
import md.stejar.transfers.clients.HttpExchangeRateClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppConfigurationTest {

    // Loads application.properties, which reads RATES_BASE_URL, RATES_API_KEY and RATES_TIMEOUT_SECONDS.
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(AppConfiguration.class);

    @Test
    void settingsAreReadFromTheEnvironmentVariables() {
        context.withSystemProperties(
                        "RATES_BASE_URL=https://rates.example.invalid",
                        "RATES_API_KEY=fake-key-do-not-use",
                        "RATES_TIMEOUT_SECONDS=2")
                .run(ctx -> assertThat(ctx.getBean(RatesProperties.class))
                        .isEqualTo(new RatesProperties("https://rates.example.invalid", "fake-key-do-not-use", 2)));
    }

    @Test
    void httpRateClientIsUsedWhenABaseUrlIsSet() {
        context.withPropertyValues("rates.base-url=https://rates.example.invalid")
                .run(ctx -> assertThat(ctx.getBean(ExchangeRateClient.class)).isInstanceOf(HttpExchangeRateClient.class));
    }

    @Test
    void fixedRateClientIsUsedWithoutABaseUrl() {
        context.withPropertyValues("rates.base-url=")
                .run(ctx -> assertThat(ctx.getBean(ExchangeRateClient.class)).isInstanceOf(FixedRateClient.class));
    }

    @Test
    void fixedRateClientReturnsThePublishedRate() {
        ExchangeRate rate = new FixedRateClient().getRate("EUR", "MDL");

        assertThat(rate.from()).isEqualTo("EUR");
        assertThat(rate.to()).isEqualTo("MDL");
        assertThat(rate.rate()).isEqualTo(new BigDecimal("19.8765"));
    }
}
