package expensetracker.pl.trade.handbook.repository;

import expensetracker.pl.trade.handbook.model.InstrumentPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstrumentPriceRepository extends JpaRepository<InstrumentPrice, UUID> {

    Optional<InstrumentPrice> findFirstByInstrumentIdOrderByAsOfDesc(UUID instrumentId);

    /** Oldest snapshot inside the trend window - the baseline the change is measured from. */
    Optional<InstrumentPrice> findFirstByInstrumentIdAndAsOfGreaterThanEqualOrderByAsOfAsc(
            UUID instrumentId, Instant from);

    List<InstrumentPrice> findByInstrumentIdAndAsOfGreaterThanEqualOrderByAsOfAsc(
            UUID instrumentId, Instant from);
}
