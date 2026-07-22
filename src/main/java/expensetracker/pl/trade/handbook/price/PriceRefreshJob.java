package expensetracker.pl.trade.handbook.price;

import expensetracker.pl.trade.handbook.model.Instrument;
import expensetracker.pl.trade.handbook.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Keeps the snapshot history growing so trends have something to read. Providers that ran out
 * of quota simply return empty, so the job is safe to run over the whole instrument list.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market-data.refresh.enabled", havingValue = "true", matchIfMissing = true)
public class PriceRefreshJob {

    private final InstrumentRepository instrumentRepository;
    private final PriceService priceService;

    @Scheduled(cron = "${market-data.refresh.cron:0 30 18 * * MON-FRI}", zone = "Europe/Berlin")
    public void refreshAll() {
        List<Instrument> instruments = instrumentRepository.findAllWithExchange();
        int ok = 0;
        for (Instrument instrument : instruments) {
            try {
                priceService.refreshPrice(instrument.getId());
                ok++;
            } catch (Exception e) {
                log.debug("Refresh skipped for {}: {}", instrument.getTicker(), e.getMessage());
            }
        }
        log.info("Price refresh finished: {}/{} instruments updated", ok, instruments.size());
    }
}
