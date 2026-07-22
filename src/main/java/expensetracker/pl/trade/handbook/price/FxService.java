package expensetracker.pl.trade.handbook.price;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ECB reference rates via Frankfurter - no key, no quota. Instruments span 12 currencies,
 * so portfolio-level sums need a common one.
 */
@Slf4j
@Service
public class FxService {

    private final MarketDataProperties.Fx config;
    private final RestClient client;

    public FxService(MarketDataProperties properties, RestClient.Builder builder) {
        this.config = properties.getFx();
        this.client = builder.baseUrl(config.getBaseUrl()).build();
    }

    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        return amount.multiply(rate(from.toUpperCase(), to.toUpperCase()))
                .setScale(4, RoundingMode.HALF_UP);
    }

    @Cacheable(cacheNames = "fxRates", key = "#from + '->' + #to")
    public BigDecimal rate(String from, String to) {
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FX conversion is disabled");
        }
        try {
            JsonNode body = client.get()
                    .uri(uri -> uri.path("/v1/latest")
                            .queryParam("base", from)
                            .queryParam("symbols", to)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode rate = body == null ? null : body.path("rates").path(to);
            if (rate == null || rate.isMissingNode()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No FX rate for " + from + "->" + to);
            }
            return rate.decimalValue();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("FX fetch failed for {}->{}: {}", from, to, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FX provider unavailable");
        }
    }
}
