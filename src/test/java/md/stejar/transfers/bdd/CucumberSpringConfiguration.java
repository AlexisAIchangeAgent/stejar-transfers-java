package md.stejar.transfers.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import md.stejar.transfers.clients.ExchangeRateClient;
import md.stejar.transfers.clients.FixedRateClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Spring context of the Cucumber scenarios: the whole service, in process, with a MockMvc to
 * call it, and the fixed-rate stub (rate 19.8765): no network call.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class FixedRate {

        @Bean
        @Primary
        ExchangeRateClient fixedRateClient() {
            return new FixedRateClient();
        }
    }
}
