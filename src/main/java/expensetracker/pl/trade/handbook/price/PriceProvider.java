package expensetracker.pl.trade.handbook.price;

import expensetracker.pl.trade.handbook.model.Instrument;

import java.util.Optional;

/**
 * One external market-data source. Implementations must never throw: a provider that is
 * down, rate-limited or simply does not know the ticker returns {@link Optional#empty()}
 * so that {@link PriceService} can fall through to the next one.
 */
public interface PriceProvider {

    /** Stable id stored in {@code instrument_prices.source}. */
    String name();

    /** Lower value wins when several providers can serve the same instrument. */
    int priority();

    boolean supports(Instrument instrument);

    Optional<PriceQuote> fetch(Instrument instrument);
}
