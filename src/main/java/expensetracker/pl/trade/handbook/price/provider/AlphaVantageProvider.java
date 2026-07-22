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
import java.time.LocalDate;
import java.util.Optional;

/**
 * Global fallback. Covers most non-German venues via ticker suffixes ({@code BP.LON},
 * {@code SAP.DEX}, {@code SHOP.TRT}), but the free key allows only 25 calls a day, so the
 * budget is guarded locally and callers must rely on the cache + stored snapshots.
 */
@Slf4j
@Component
public class AlphaVantageProvider implements PriceProvider {

    private final MarketDataProperties.AlphaVantage config;
    private final RestClient client;

    private LocalDate budgetDay = LocalDate.now();
    private int callsToday = 0;

    public AlphaVantageProvider(MarketDataProperties properties, RestClient.Builder builder) {
        this.config = properties.getAlphaVantage();
        this.client = builder.baseUrl(config.getBaseUrl()).build();
    }

    @Override
    public String name() {
        return "alpha-vantage";
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public boolean supports(Instrument instrument) {
        return config.isEnabled()
                && config.getApiKey() != null && !config.getApiKey().isBlank()
                && config.getSuffixMap().containsKey(instrument.getExchange().getSuffix());
    }

    @Override
    public Optional<PriceQuote> fetch(Instrument instrument) {
        if (!reserveCall()) {
            log.info("{}: daily budget of {} calls exhausted, skipping {}",
                    name(), config.getDailyLimit(), instrument.getTicker());
            return Optional.empty();
        }

        String symbol = instrument.getTicker() + config.getSuffixMap().get(instrument.getExchange().getSuffix());
        try {
            JsonNode body = client.get()
                    .uri(uri -> uri.path("/query")
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", config.getApiKey())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode quote = body == null ? null : body.path("Global Quote");
            if (quote == null || quote.isMissingNode() || quote.isEmpty()) {
                // Alpha Vantage answers 200 with {"Information": "..."} on throttling / unknown symbol.
                log.warn("{}: no quote for {} ({})", name(), symbol,
                        body == null ? "empty body" : body.path("Information").asText("unknown symbol"));
                return Optional.empty();
            }

            return Optional.of(new PriceQuote(
                    new BigDecimal(quote.path("05. price").asText()),
                    new BigDecimal(quote.path("08. previous close").asText()),
                    instrument.getCurrency(),
                    Instant.now(),
                    name()
            ));
        } catch (Exception e) {
            log.warn("{}: fetch failed for {}: {}", name(), symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private synchronized boolean reserveCall() {
        LocalDate today = LocalDate.now();
        if (!today.equals(budgetDay)) {
            budgetDay = today;
            callsToday = 0;
        }
        if (callsToday >= config.getDailyLimit()) {
            return false;
        }
        callsToday++;
        return true;
    }
}
