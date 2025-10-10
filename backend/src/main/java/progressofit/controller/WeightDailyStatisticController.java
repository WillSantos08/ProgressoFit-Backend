package progressofit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import progressofit.infra.security.AuthUtil;
import progressofit.model.weightdata.WeightDailyStatistic;
import progressofit.model.weightdata.dto.WeeklyWeightDTO;
import progressofit.service.WeightDailyStatisticService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/weight")
@CrossOrigin(origins = "*")
public class WeightDailyStatisticController {

    @Autowired
    private WeightDailyStatisticService service;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<WeightDailyStatistic>> getWeightStatisticsUser() {
        List<WeightDailyStatistic> statistics = service.findByUserId(this.authUtil.getCurrentUserId());
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WeightDailyStatistic>> getWeightStatisticsAllUsers() {
        List<WeightDailyStatistic> statistics = service.findAll();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeightDailyStatistic> getWeightStatisticById(@PathVariable Long id) {
        Optional<WeightDailyStatistic> statistic = service.findById(id);
        return statistic.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/period")
    public ResponseEntity<List<WeightDailyStatistic>> getWeightStatisticsByPeriod(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Long userId = authUtil.getCurrentUserId();
        List<WeightDailyStatistic> statistics = service.findByUserIdAndDateBetween(userId, startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<WeightDailyStatistic> getWeightStatisticByDate(@PathVariable LocalDate date) {
        Long userId = authUtil.getCurrentUserId();
        Optional<WeightDailyStatistic> statistic = service.findByUserIdAndDate(userId, date);
        return statistic.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/current-month")
    public ResponseEntity<List<WeightDailyStatistic>> getWeightStatisticsCurrentMonth() {
        Long userId = authUtil.getCurrentUserId();
        List<WeightDailyStatistic> statistics = service.findByUserIdCurrentMonth(userId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/current-week")
    public ResponseEntity<List<WeightDailyStatistic>> getWeightStatisticsCurrentWeek() {
        Long userId = authUtil.getCurrentUserId();
        List<WeightDailyStatistic> statistics = service.findByUserIdCurrentWeek(userId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/last/{limit}")
    public ResponseEntity<List<WeightDailyStatistic>> getLastNWeightStatistics(@PathVariable int limit) {
        Long userId = authUtil.getCurrentUserId();
        List<WeightDailyStatistic> statistics = service.findLastNByUserId(userId, limit);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/latest")
    public ResponseEntity<WeightDailyStatistic> getLatestWeight() {
        Long userId = authUtil.getCurrentUserId();
        Optional<WeightDailyStatistic> latestWeight = service.findLatestWeightByUserId(userId);
        return latestWeight.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/average")
    public ResponseEntity<BigDecimal> getAverageWeight(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Long userId = authUtil.getCurrentUserId();
        Optional<BigDecimal> averageWeight = service.findAverageWeightByPeriod(userId, startDate, endDate);
        return averageWeight.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<WeightDailyStatistic> createWeightStatistic(@RequestBody WeightDailyStatistic statistic) {
        try {
            statistic.setUserId(authUtil.getCurrentUserId());
            WeightDailyStatistic savedStatistic = service.save(statistic);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedStatistic);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeightDailyStatistic> updateWeightStatistic(
            @PathVariable Long id,
            @RequestBody WeightDailyStatistic updatedStatistic) {

        Optional<WeightDailyStatistic> oldStatisticOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (oldStatisticOpt.isEmpty() ||
                !Objects.equals(oldStatisticOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            updatedStatistic.setId(id);
            updatedStatistic.setUserId(userId);
            WeightDailyStatistic newStatistic = service.update(updatedStatistic);
            return ResponseEntity.ok(newStatistic);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/date/{date}")
    public ResponseEntity<WeightDailyStatistic> upsertWeightStatistic(
            @PathVariable LocalDate date,
            @RequestBody WeightDailyStatistic statistic) {

        Long userId = authUtil.getCurrentUserId();

        try {
            statistic.setUserId(userId);
            statistic.setDate(date);

            WeightDailyStatistic savedStatistic = service.upsertByUserIdAndDate(statistic);
            return ResponseEntity.ok(savedStatistic);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeightStatistic(@PathVariable Long id) {
        Optional<WeightDailyStatistic> statisticOpt = service.findById(id);
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
    public ResponseEntity<Void> deleteWeightStatisticByDate(@PathVariable LocalDate date) {
        Long userId = authUtil.getCurrentUserId();

        try {
            boolean deleted = service.deleteByUserIdAndDate(userId, date);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllWeightStatisticsByUser(@PathVariable Long userId) {
        service.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/weekly/period")
    public ResponseEntity<List<WeeklyWeightDTO>> getWeeklyWeightStatistics(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Long userId = authUtil.getCurrentUserId();
        List<WeeklyWeightDTO> weeklyStats = service.findWeeklyWeightStatistics(userId, startDate, endDate);
        return ResponseEntity.ok(weeklyStats);
    }

    @GetMapping("/weekly/last-months/{monthsBack}")
    public ResponseEntity<List<WeeklyWeightDTO>> getWeeklyWeightStatisticsLastMonths(
            @PathVariable int monthsBack) {
        Long userId = authUtil.getCurrentUserId();
        List<WeeklyWeightDTO> weeklyStats = service.findWeeklyWeightStatisticsLastMonths(userId, monthsBack);
        return ResponseEntity.ok(weeklyStats);
    }

    @GetMapping("/weekly/current-year")
    public ResponseEntity<List<WeeklyWeightDTO>> getWeeklyWeightStatisticsCurrentYear() {
        Long userId = authUtil.getCurrentUserId();
        List<WeeklyWeightDTO> weeklyStats = service.findWeeklyWeightStatisticsCurrentYear(userId);
        return ResponseEntity.ok(weeklyStats);
    }
}