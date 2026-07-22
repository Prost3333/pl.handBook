package expensetracker.pl.trade.handbook.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceResponse(
        UUID instrumentId,
        String ticker,
        String exchangeName,
        BigDecimal price,
        BigDecimal previousClose,
        BigDecimal changeAbsolute,
        BigDecimal changePercent,
        String currency,
        String source,
        Instant asOf,
        boolean stale
) implements Serializable {
}
