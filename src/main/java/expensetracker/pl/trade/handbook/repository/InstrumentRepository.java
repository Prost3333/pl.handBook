package expensetracker.pl.trade.handbook.repository;

import expensetracker.pl.trade.handbook.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
    @Query("SELECT i FROM Instrument i JOIN FETCH i.exchange e")
    List<Instrument> findAllWithExchange();
}
