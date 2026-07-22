package expensetracker.pl.trade.handbook.dto;

import java.util.List;
import java.util.UUID;

public record InstrumentWithCategoryResp(UUID id, String ticker, String name
        , String currency, String exchangeName, List<String> categories, String isin) {}
