package expensetracker.pl.trade.handbook.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Provider-agnostic quote. Every {@link PriceProvider} maps its own payload into this.
 */
public record PriceQuote(
        BigDecimal price,
        BigDecimal previousClose,
        String currency,
        Instant asOf,
        String source
) {
    public BigDecimal changeAbsolute() {
        if (previousClose == null) {
            return null;
        }
        return price.subtract(previousClose);
    }

    public BigDecimal changePercent() {
        if (previousClose == null || previousClose.signum() == 0) {
            return null;
        }
        return changeAbsolute()
                .divide(previousClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
