package md.stejar.transfers.clients;

/**
 * Exchange-rate provider (third party) that gives the EUR to MDL rate.
 *
 * <p>Contract of the provider:
 *
 * <pre>
 * GET {base_url}/rates?from=EUR&amp;to=MDL
 * 200 {"from": "EUR", "to": "MDL", "rate": 19.8765, "as_of": "2026-10-19"}
 * </pre>
 */
public interface ExchangeRateClient {

    /**
     * Returns the current rate.
     *
     * @throws ExchangeRateException if the rate cannot be obtained
     */
    ExchangeRate getRate(String from, String to);
}
