package expensetracker.pl.trade.handbook.dto;

import java.util.UUID;

public record InstrumentResponse(UUID id, String ticker, String name
        , String currency, String exchangeName,String isin) {}
