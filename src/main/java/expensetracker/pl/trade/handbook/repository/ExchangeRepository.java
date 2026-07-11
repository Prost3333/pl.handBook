package expensetracker.pl.trade.handbook.repository;

import expensetracker.pl.trade.handbook.model.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, UUID> {
}
