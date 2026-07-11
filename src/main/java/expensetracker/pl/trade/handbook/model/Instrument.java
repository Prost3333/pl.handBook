package expensetracker.pl.trade.handbook.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name ="instruments")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Instrument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false,unique = true)
    private String ticker;
    @Column(nullable = false,length = 3)
    private String currency;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;
}
