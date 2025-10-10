package progressofit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import progressofit.infra.security.AuthUtil;
import progressofit.model.goal.Goal;
import progressofit.service.GoalService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = "*")
public class GoalController {

    @Autowired
    private GoalService service;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<Goal>> getGoalsUser() {
        List<Goal> goals = service.findByUserId(authUtil.getCurrentUserId());
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Goal>> getActiveGoals() {
        Long userId = authUtil.getCurrentUserId();
        List<Goal> goals = service.findActiveGoalsByUserId(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<Goal>> getExpiredGoals() {
        Long userId = authUtil.getCurrentUserId();
        List<Goal> goals = service.findExpiredGoalsByUserId(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/label/{label}")
    public ResponseEntity<Goal> getGoalByLabel(@PathVariable String label) {
        Long userId = authUtil.getCurrentUserId();
        Optional<Goal> goal = service.findByUserIdAndLabel(userId, label);
        return goal.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/period/{periodDays}")
    public ResponseEntity<List<Goal>> getGoalsByPeriod(@PathVariable Integer periodDays) {
        Long userId = authUtil.getCurrentUserId();
        List<Goal> goals = service.findByUserIdAndPeriod(userId, periodDays);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/unit/{valueUnit}")
    public ResponseEntity<List<Goal>> getGoalsByValueUnit(@PathVariable String valueUnit) {
        Long userId = authUtil.getCurrentUserId();
        List<Goal> goals = service.findByUserIdAndValueUnit(userId, valueUnit);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/start-date/{startDate}")
    public ResponseEntity<List<Goal>> getGoalsByStartDate(@PathVariable LocalDate startDate) {
        Long userId = authUtil.getCurrentUserId();
        List<Goal> goals = service.findByUserIdAndStartDate(userId, startDate);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/start-date-range")
    public ResponseEntity<List<Goal>> getGoalsByStartDateRange(
            @RequestParam LocalDate startFrom,
            @RequestParam LocalDate startTo) {
        Long userId = authUtil.getCurrentUserId();
        List<Goal> goals = service.findByUserIdAndStartDateBetween(userId, startFrom, startTo);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<List<Goal>> getGoalsExpiringSoon(@RequestParam(defaultValue = "7") int daysAhead) {
        Long userId = authUtil.getCurrentUserId();
        List<Goal> goals = service.findGoalsExpiringSoon(userId, daysAhead);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/high-streak")
    public ResponseEntity<List<Goal>> getGoalsWithHighStreak(@RequestParam(defaultValue = "7") Integer minStreak) {
        Long userId = authUtil.getCurrentUserId();
        List<Goal> goals = service.findGoalsWithHighStreak(userId, minStreak);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> countActiveGoals() {
        Long userId = authUtil.getCurrentUserId();
        long count = service.countActiveGoalsByUserId(userId);
        return ResponseEntity.ok(count);
    }

    @PostMapping
    public ResponseEntity<Goal> createGoal(@RequestBody Goal goal) {
        try {
            goal.setUserId(authUtil.getCurrentUserId());

            // Verifica se já existe um objetivo com o mesmo label
            if (service.existsByUserIdAndLabel(goal.getUserId(), goal.getLabel())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            // Se startDate não foi fornecida, usa a data atual
            if (goal.getStartDate() == null) {
                goal.setStartDate(LocalDate.now());
            }

            Goal savedGoal = service.save(goal);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Goal> updateGoal(
            @PathVariable Long id,
            @RequestBody Goal updatedGoal) {

        Optional<Goal> oldGoalOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (oldGoalOpt.isEmpty() ||
                !Objects.equals(oldGoalOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Verifica se o novo label já existe em outro objetivo
            Goal oldGoal = oldGoalOpt.get();
            if (!oldGoal.getLabel().equals(updatedGoal.getLabel()) &&
                    service.existsByUserIdAndLabel(userId, updatedGoal.getLabel())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            updatedGoal.setId(id);
            updatedGoal.setUserId(userId);

            // Mantém a startDate original se não for fornecida
            if (updatedGoal.getStartDate() == null) {
                updatedGoal.setStartDate(oldGoal.getStartDate());
            }

            Goal newGoal = service.update(updatedGoal);
            return ResponseEntity.ok(newGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/label/{label}")
    public ResponseEntity<Goal> upsertGoalByLabel(
            @PathVariable String label,
            @RequestBody Goal goal) {

        Long userId = authUtil.getCurrentUserId();

        try {
            goal.setUserId(userId);
            goal.setLabel(label);

            Optional<Goal> existingGoal = service.findByUserIdAndLabel(userId, label);

            if (existingGoal.isPresent()) {
                // Atualiza o objetivo existente
                goal.setId(existingGoal.get().getId());

                // Mantém a startDate original se não for fornecida
                if (goal.getStartDate() == null) {
                    goal.setStartDate(existingGoal.get().getStartDate());
                }

                Goal updatedGoal = service.update(goal);
                return ResponseEntity.ok(updatedGoal);
            } else {
                // Cria um novo objetivo
                if (goal.getStartDate() == null) {
                    goal.setStartDate(LocalDate.now());
                }

                Goal savedGoal = service.save(goal);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedGoal);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PatchMapping("/{id}/streak/increment")
    public ResponseEntity<Goal> incrementStreak(@PathVariable Long id) {
        Optional<Goal> goalOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (goalOpt.isEmpty() ||
                !Objects.equals(goalOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Goal updatedGoal = service.incrementStreak(id);
            return ResponseEntity.ok(updatedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PatchMapping("/{id}/streak/reset")
    public ResponseEntity<Goal> resetStreak(@PathVariable Long id) {
        Optional<Goal> goalOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (goalOpt.isEmpty() ||
                !Objects.equals(goalOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Goal updatedGoal = service.resetStreak(id);
            return ResponseEntity.ok(updatedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PatchMapping("/{id}/streak")
    public ResponseEntity<Goal> updateStreak(
            @PathVariable Long id,
            @RequestParam Integer newStreak) {

        Optional<Goal> goalOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (goalOpt.isEmpty() ||
                !Objects.equals(goalOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Goal updatedGoal = service.updateStreak(id, newStreak);
            return ResponseEntity.ok(updatedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PatchMapping("/{id}/start-date")
    public ResponseEntity<Goal> updateStartDate(
            @PathVariable Long id,
            @RequestParam LocalDate startDate) {

        Optional<Goal> goalOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (goalOpt.isEmpty() ||
                !Objects.equals(goalOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Goal updatedGoal = service.updateStartDate(id, startDate);
            return ResponseEntity.ok(updatedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PatchMapping("/{id}/end-date")
    public ResponseEntity<Goal> updateEndDate(
            @PathVariable Long id,
            @RequestParam LocalDate endDate) {

        Optional<Goal> goalOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (goalOpt.isEmpty() ||
                !Objects.equals(goalOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Goal updatedGoal = service.updateEndDate(id, endDate);
            return ResponseEntity.ok(updatedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/{id}/end-date")
    public ResponseEntity<Goal> removeEndDate(@PathVariable Long id) {
        Optional<Goal> goalOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (goalOpt.isEmpty() ||
                !Objects.equals(goalOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Goal updatedGoal = service.removeEndDate(id);
            return ResponseEntity.ok(updatedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        Optional<Goal> goalOpt = service.findById(id);
        Long userId = authUtil.getCurrentUserId();

        if (goalOpt.isEmpty() ||
                !Objects.equals(goalOpt.get().getUserId(), userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Goal>> getGoalsAllUsers() {
        List<Goal> goals = service.findAll();
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Goal> getGoalById(@PathVariable Long id) {
        Optional<Goal> goal = service.findById(id);
        return goal.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllGoalsByUser(@PathVariable Long userId) {
        service.deleteAllByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}