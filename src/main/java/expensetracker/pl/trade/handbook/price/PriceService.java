package expensetracker.pl.trade.handbook.price;

import expensetracker.pl.trade.handbook.dto.PriceResponse;
import expensetracker.pl.trade.handbook.dto.TrendResponse;
import expensetracker.pl.trade.handbook.model.Instrument;
import expensetracker.pl.trade.handbook.model.InstrumentPrice;
import expensetracker.pl.trade.handbook.repository.InstrumentPriceRepository;
import expensetracker.pl.trade.handbook.repository.InstrumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fans an instrument out to whichever free provider can serve it, stores every answer as a
 * snapshot, and answers from the last snapshot (flagged {@code stale}) when every provider fails.
 */
@Slf4j
@Service
public class PriceService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final List<PriceProvider> providers;

    public PriceService(InstrumentRepository instrumentRepository,
                        InstrumentPriceRepository priceRepository,
                        List<PriceProvider> providers) {
        this.instrumentRepository = instrumentRepository;
        this.priceRepository = priceRepository;
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(PriceProvider::priority))
                .toList();
    }

    @Cacheable(cacheNames = "quotes", key = "#instrumentId")
    public PriceResponse getPrice(UUID instrumentId) {
        return load(instrumentId);
    }

    /** Bypasses the cache and overwrites it - used by the scheduled refresh and by the API. */
    @CachePut(cacheNames = "quotes", key = "#instrumentId")
    public PriceResponse refreshPrice(UUID instrumentId) {
        return load(instrumentId);
    }

    public TrendResponse getTrend(UUID instrumentId, int days) {
        Instrument instrument = requireInstrument(instrumentId);
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);

        List<InstrumentPrice> window =
                priceRepository.findByInstrumentIdAndAsOfGreaterThanEqualOrderByAsOfAsc(instrumentId, from);
        if (window.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No price history for " + instrument.getTicker() + " in the last " + days + " days");
        }

        InstrumentPrice first = window.get(0);
        InstrumentPrice last = window.get(window.size() - 1);
        BigDecimal change = last.getPrice().subtract(first.getPrice());

        return new TrendResponse(
                instrumentId,
                instrument.getTicker(),
                days,
                first.getPrice(),
                last.getPrice(),
                change,
                percent(change, first.getPrice()),
                last.getCurrency(),
                first.getAsOf(),
                last.getAsOf(),
                window.size()
        );
    }

    private PriceResponse load(UUID instrumentId) {
        Instrument instrument = requireInstrument(instrumentId);

        for (PriceProvider provider : providers) {
            if (!provider.supports(instrument)) {
                continue;
            }
            Optional<PriceQuote> quote = provider.fetch(instrument);
            if (quote.isPresent()) {
                InstrumentPrice saved = store(instrument, quote.get());
                return toResponse(instrument, saved, false);
            }
        }

        return priceRepository.findFirstByInstrumentIdOrderByAsOfDesc(instrumentId)
                .map(snapshot -> {
                    log.warn("All providers failed for {}, serving snapshot from {}",
                            instrument.getTicker(), snapshot.getAsOf());
                    return toResponse(instrument, snapshot, true);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No price available for " + instrument.getTicker()));
    }

    private InstrumentPrice store(Instrument instrument, PriceQuote quote) {
        return priceRepository.save(InstrumentPrice.builder()
                .instrument(instrument)
                .price(quote.price())
                .previousClose(quote.previousClose())
                .currency(quote.currency())
                .source(quote.source())
                .asOf(quote.asOf())
                .build());
    }

    private Instrument requireInstrument(UUID instrumentId) {
        return instrumentRepository.findByIdWithExchange(instrumentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Instrument not found: " + instrumentId));
    }

    private PriceResponse toResponse(Instrument instrument, InstrumentPrice price, boolean stale) {
        BigDecimal change = price.getPreviousClose() == null
                ? null
                : price.getPrice().subtract(price.getPreviousClose());

        return new PriceResponse(
                instrument.getId(),
                instrument.getTicker(),
                instrument.getExchange().getName(),
                price.getPrice(),
                price.getPreviousClose(),
                change,
                change == null ? null : percent(change, price.getPreviousClose()),
                price.getCurrency(),
                price.getSource(),
                price.getAsOf(),
                stale
        );
    }

    private BigDecimal percent(BigDecimal change, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return null;
        }
        return change.divide(base, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
