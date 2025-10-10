package progressofit.model.trainingdata.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTrainingCountDTO {

    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private Integer totalTrainings;

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