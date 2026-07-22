package expensetracker.pl.trade.handbook.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "instrument_prices")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
    @Column(name = "previous_close", precision = 19, scale = 4)
    private BigDecimal previousClose;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false, length = 64)
    private String source;
    @Column(name = "as_of", nullable = false)
    private Instant asOf;
}
