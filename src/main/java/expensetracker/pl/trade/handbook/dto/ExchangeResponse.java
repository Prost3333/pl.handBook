package expensetracker.pl.trade.handbook.dto;

import java.util.UUID;

public record ExchangeResponse(UUID id, String name, String suffix, String country) {}
