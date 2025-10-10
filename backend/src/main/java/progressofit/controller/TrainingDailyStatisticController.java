package progressofit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import progressofit.infra.security.AuthUtil;
import progressofit.model.trainingdata.TrainingDailyStatistic;
import progressofit.service.TrainingDailyStatisticService;
import progressofit.model.trainingdata.dto.WeeklyTrainingCountDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class TrainingDailyStatisticController {

    @Autowired
    private TrainingDailyStatisticService service;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<TrainingDailyStatistic>> getStatisticsUser() {
        List<TrainingDailyStatistic> statistics = service.findByUserId(this.authUtil.getCurrentUserId());
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/period")
    public ResponseEntity<List<TrainingDailyStatistic>> getStatisticsByPeriod(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Long userId = authUtil.getCurrentUserId();
        List<TrainingDailyStatistic> statistics = service.findByUserIdAndDateBetween(userId, startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<TrainingDailyStatistic> getStatisticByDate(@PathVariable LocalDate date) {
        Long userId = authUtil.getCurrentUserId();
        Optional<TrainingDailyStatistic> statistic = service.findByUserIdAndDate(userId, date);
        return statistic.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/current-month")
    public ResponseEntity<List<TrainingDailyStatistic>> getStatisticsCurrentMonth() {
        Long userId = authUtil.getCurrentUserId();
        List<TrainingDailyStatistic> statistics = service.findByUserIdCurrentMonth(userId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/current-week")
    public ResponseEntity<List<TrainingDailyStatistic>> getStatisticsCurrentWeek() {
        Long userId = authUtil.getCurrentUserId();
        List<TrainingDailyStatistic> statistics = service.findByUserIdCurrentWeek(userId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/last/{limit}")
    public ResponseEntity<List<TrainingDailyStatistic>> getLastNStatistics(@PathVariable int limit) {
        Long userId = authUtil.getCurrentUserId();
        List<TrainingDailyStatistic> statistics = service.findLastNByUserId(userId, limit);
        return ResponseEntity.ok(statistics);
    }

    @PostMapping
    public ResponseEntity<TrainingDailyStatistic> createStatistic(@RequestBody TrainingDailyStatistic statistic) {
        try {
            statistic.setUserId(authUtil.getCurrentUserId());
            TrainingDailyStatistic savedStatistic = service.save(statistic);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedStatistic);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingDailyStatistic> updateStatistic(
            @PathVariable Long id,
            @RequestBody TrainingDailyStatistic updatedStatistic) {

        Optional<TrainingDailyStatistic> oldStatisticOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (oldStatisticOpt.isEmpty() ||
                !Objects.equals(oldStatisticOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            updatedStatistic.setId(id);
            updatedStatistic.setUserId(userId);
            TrainingDailyStatistic newStatistic = service.update(updatedStatistic);
            return ResponseEntity.ok(newStatistic);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/date/{date}")
    public ResponseEntity<TrainingDailyStatistic> upsertStatistic(
            @PathVariable LocalDate date,
            @RequestBody TrainingDailyStatistic statistic) {

        Long userId = authUtil.getCurrentUserId();

        try {
            statistic.setUserId(userId);
            statistic.setDate(date);

            TrainingDailyStatistic savedStatistic = service.upsertByUserIdAndDate(statistic);
            return ResponseEntity.ok(savedStatistic);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatistic(@PathVariable Long id) {
        Optional<TrainingDailyStatistic> statisticOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (statisticOpt.isEmpty() ||
                !Objects.equals(statisticOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/date/{date}")
    public ResponseEntity<Void> deleteStatisticByDate(@PathVariable LocalDate date) {
        Long userId = authUtil.getCurrentUserId();

        try {
            boolean deleted = service.deleteByUserIdAndDate(userId, date);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/weekly/period")
    public ResponseEntity<List<WeeklyTrainingCountDTO>> getWeeklyTrainingCounts(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Long userId = authUtil.getCurrentUserId();
        List<WeeklyTrainingCountDTO> weeklyCounts = service.findWeeklyTrainingCounts(userId, startDate, endDate);
        return ResponseEntity.ok(weeklyCounts);
    }

    @GetMapping("/weekly/last-months/{monthsBack}")
    public ResponseEntity<List<WeeklyTrainingCountDTO>> getWeeklyTrainingCountsLastMonths(
            @PathVariable int monthsBack) {
        Long userId = authUtil.getCurrentUserId();
        List<WeeklyTrainingCountDTO> weeklyCounts = service.findWeeklyTrainingCountsLastMonths(userId, monthsBack);
        return ResponseEntity.ok(weeklyCounts);
    }

    @GetMapping("/weekly/current-year")
    public ResponseEntity<List<WeeklyTrainingCountDTO>> getWeeklyTrainingCountsCurrentYear() {
        Long userId = authUtil.getCurrentUserId();
        List<WeeklyTrainingCountDTO> weeklyCounts = service.findWeeklyTrainingCountsCurrentYear(userId);
        return ResponseEntity.ok(weeklyCounts);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TrainingDailyStatistic>> getStatisticsAllUsers() {
        List<TrainingDailyStatistic> statistics = service.findAll();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrainingDailyStatistic> getStatisticById(@PathVariable Long id) {
        Optional<TrainingDailyStatistic> statistic = service.findById(id);
        return statistic.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllStatisticsByUser(@PathVariable Long userId) {
        service.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}