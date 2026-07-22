package expensetracker.pl.trade.handbook.controller;

import expensetracker.pl.trade.handbook.dto.PriceResponse;
import expensetracker.pl.trade.handbook.dto.TrendResponse;
import expensetracker.pl.trade.handbook.price.FxService;
import expensetracker.pl.trade.handbook.price.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/instrument/{id}")
@RequiredArgsConstructor
@Tag(name = "Prices", description = "Market prices and trends from free data providers")
public class PriceController {

    private final PriceService priceService;
    private final FxService fxService;

    @GetMapping("/price")
    @Operation(summary = "Current price (cached; falls back to the last stored snapshot)")
    public PriceResponse getPrice(@PathVariable UUID id) {
        return priceService.getPrice(id);
    }

    @PostMapping("/price/refresh")
    @Operation(summary = "Force a provider call and overwrite the cached price")
    public PriceResponse refreshPrice(@PathVariable UUID id) {
        return priceService.refreshPrice(id);
    }

    @GetMapping("/trend")
    @Operation(summary = "Price change over the last N days, computed from stored snapshots")
    public TrendResponse getTrend(@PathVariable UUID id, @RequestParam(defaultValue = "30") int days) {
        return priceService.getTrend(id, days);
    }

    @GetMapping("/price/converted")
    @Operation(summary = "Current price converted into the requested currency (ECB rates)")
    public BigDecimal getConvertedPrice(@PathVariable UUID id, @RequestParam String currency) {
        PriceResponse price = priceService.getPrice(id);
        return fxService.convert(price.price(), price.currency(), currency);
    }
}
