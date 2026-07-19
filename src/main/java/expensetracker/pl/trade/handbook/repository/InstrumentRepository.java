package expensetracker.pl.trade.handbook.repository;

import expensetracker.pl.trade.handbook.model.Exchange;
import expensetracker.pl.trade.handbook.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
    @Query("SELECT i FROM Instrument i JOIN FETCH i.exchange e")
    List<Instrument> findAllWithExchange();


    @Query("""
    SELECT DISTINCT i
    FROM Instrument i
    JOIN FETCH i.exchange
    LEFT JOIN FETCH i.categories
    """)
    List<Instrument> findAllWithCategories();

    @Query("SELECT i FROM Instrument i JOIN FETCH i.exchange e WHERE i.id = :id")
    Optional<Instrument> findByIdWithExchange(@Param("id") UUID id);

    Optional<Instrument> findByTickerAndExchange(String ticker, Exchange exchange);

    boolean existsByExchange(Exchange exchange);
}
