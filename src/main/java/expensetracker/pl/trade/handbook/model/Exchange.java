package expensetracker.pl.trade.handbook.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name ="exchanges")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Exchange {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private  String suffix;
    @Column(nullable = false)
    private  String country;
}
