package expensetracker.pl.trade.handbook.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "instruments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ticker", "exchange_id"}))
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
    @Column(nullable = false)
    private String ticker;
    @Column(nullable = false,length = 3)
    private String currency;
    @Column(length = 12)
    private String isin;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "instrument_category",
            joinColumns = @JoinColumn(name = "instrument_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    public void addCategory(Category category) {
        this.categories.add(category);
        category.getInstruments().add(this);
    }

    public void removeCategory(Category category) {
        this.categories.remove(category);
        category.getInstruments().remove(this);
    }
}
