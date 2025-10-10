package progressofit.model.goal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "goals", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "label"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "target_value", nullable = false)
    private Float targetValue;

    @Column(name = "value_unit", nullable = false)
    private String valueUnit;

    @Column(name = "period_days")
    private Integer periodDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;
}