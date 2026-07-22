package expensetracker.pl.trade.handbook.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrendResponse(
        UUID instrumentId,
        String ticker,
        int days,
        BigDecimal from,
        BigDecimal to,
        BigDecimal changeAbsolute,
        BigDecimal changePercent,
        String currency,
        Instant firstAsOf,
        Instant lastAsOf,
        int samples
) {
}
