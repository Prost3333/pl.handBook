package expensetracker.pl.trade.handbook.price.provider;

import com.fasterxml.jackson.databind.JsonNode;
import expensetracker.pl.trade.handbook.model.Instrument;
import expensetracker.pl.trade.handbook.price.MarketDataProperties;
import expensetracker.pl.trade.handbook.price.PriceProvider;
import expensetracker.pl.trade.handbook.price.PriceQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Unofficial Boerse Frankfurt endpoint: no key, no quota, real-time, but German venues only
 * and it addresses instruments by ISIN. Keep it first - it saves the scarce Alpha Vantage quota.
 */
@Slf4j
@Component
public class BoerseFrankfurtProvider implements PriceProvider {

    private final MarketDataProperties.BoerseFrankfurt config;
    private final RestClient client;

    public BoerseFrankfurtProvider(MarketDataProperties properties, RestClient.Builder builder) {
        this.config = properties.getBoerseFrankfurt();
        this.client = builder.baseUrl(config.getBaseUrl()).build();
    }

    @Override
    public String name() {
        return "boerse-frankfurt";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean supports(Instrument instrument) {
        return config.isEnabled()
                && instrument.getIsin() != null
                && config.getMicMap().containsKey(instrument.getExchange().getSuffix());
    }

    @Override
    public Optional<PriceQuote> fetch(Instrument instrument) {
        String mic = config.getMicMap().get(instrument.getExchange().getSuffix());
        try {
            JsonNode body = client.get()
                    .uri(uri -> uri.path("/v1/data/quote_box/single")
                            .queryParam("isin", instrument.getIsin())
                            .queryParam("mic", mic)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (body == null || body.path("lastPrice").isMissingNode()) {
                return Optional.empty();
            }

            BigDecimal price = body.path("lastPrice").decimalValue();
            BigDecimal change = body.path("changeToPrevDayAbsolute").isMissingNode()
                    ? null
                    : body.path("changeToPrevDayAbsolute").decimalValue();

            return Optional.of(new PriceQuote(
                    price,
                    change == null ? null : price.subtract(change),
                    instrument.getCurrency(),
                    Instant.now(),
                    name()
            ));
        } catch (Exception e) {
            log.warn("{}: fetch failed for {} ({}): {}", name(), instrument.getTicker(), mic, e.getMessage());
            return Optional.empty();
        }
    }
}
