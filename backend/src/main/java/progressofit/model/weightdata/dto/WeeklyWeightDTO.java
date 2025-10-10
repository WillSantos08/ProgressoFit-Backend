package progressofit.model.weightdata.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyWeightDTO {

    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private BigDecimal averageWeight;
    private BigDecimal minWeight;
    private BigDecimal maxWeight;
    private Integer recordCount;

    public String getWeekDescription() {
        return String.format("Semana de %02d/%02d/%d a %02d/%02d/%d",
                weekStartDate.getDayOfMonth(),
                weekStartDate.getMonthValue(),
                weekStartDate.getYear(),
                weekEndDate.getDayOfMonth(),
                weekEndDate.getMonthValue(),
                weekEndDate.getYear()
        );
    }

    public int getWeekOfYear() {
        return weekStartDate.getDayOfYear() / 7 + 1;
    }
}