package progressofit.service;

import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progressofit.model.goal.Goal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GoalService extends GenericCrudService<Goal, Long> {

    /**
     * Busca todos os objetivos de um usuário
     *
     * @param userId ID do usuário
     * @return Lista de objetivos do usuário
     */
    @Transactional(readOnly = true)
    public List<Goal> findByUserId(Long userId) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId ORDER BY g.id DESC";
        return executeQuery(jpql, "userId", userId);
    }

    /**
     * Busca um objetivo específico de um usuário pelo label
     *
     * @param userId ID do usuário
     * @param label  Label do objetivo
     * @return Optional contendo o objetivo ou vazio
     */
    @Transactional(readOnly = true)
    public Optional<Goal> findByUserIdAndLabel(Long userId, String label) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId AND g.label = :label";
        List<Goal> results = executeQuery(jpql, "userId", userId, "label", label);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Busca objetivos ativos de um usuário (sem data final ou com data final futura)
     *
     * @param userId ID do usuário
     * @return Lista de objetivos ativos
     */
    @Transactional(readOnly = true)
    public List<Goal> findActiveGoalsByUserId(Long userId) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId " +
                "AND (g.endDate IS NULL OR g.endDate >= :today) " +
                "ORDER BY g.id DESC";
        return executeQuery(jpql, "userId", userId, "today", LocalDate.now());
    }

    /**
     * Busca objetivos expirados de um usuário
     *
     * @param userId ID do usuário
     * @return Lista de objetivos expirados
     */
    @Transactional(readOnly = true)
    public List<Goal> findExpiredGoalsByUserId(Long userId) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId " +
                "AND g.endDate IS NOT NULL AND g.endDate < :today " +
                "ORDER BY g.endDate DESC";
        return executeQuery(jpql, "userId", userId, "today", LocalDate.now());
    }

    /**
     * Busca objetivos por período (semanal, mensal, etc)
     *
     * @param userId     ID do usuário
     * @param periodDays Período em dias
     * @return Lista de objetivos com o período especificado
     */
    @Transactional(readOnly = true)
    public List<Goal> findByUserIdAndPeriod(Long userId, Integer periodDays) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId " +
                "AND g.periodDays = :periodDays " +
                "ORDER BY g.id DESC";
        return executeQuery(jpql, "userId", userId, "periodDays", periodDays);
    }

    /**
     * Busca objetivos que iniciaram em uma data específica
     *
     * @param userId    ID do usuário
     * @param startDate Data de início
     * @return Lista de objetivos que iniciaram na data especificada
     */
    @Transactional(readOnly = true)
    public List<Goal> findByUserIdAndStartDate(Long userId, LocalDate startDate) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId " +
                "AND g.startDate = :startDate " +
                "ORDER BY g.id DESC";
        return executeQuery(jpql, "userId", userId, "startDate", startDate);
    }

    /**
     * Busca objetivos que iniciaram em um período
     *
     * @param userId    ID do usuário
     * @param startFrom Data inicial do período
     * @param startTo   Data final do período
     * @return Lista de objetivos que iniciaram no período
     */
    @Transactional(readOnly = true)
    public List<Goal> findByUserIdAndStartDateBetween(Long userId, LocalDate startFrom, LocalDate startTo) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId " +
                "AND g.startDate BETWEEN :startFrom AND :startTo " +
                "ORDER BY g.startDate DESC";
        return executeQuery(jpql, "userId", userId, "startFrom", startFrom, "startTo", startTo);
    }

    /**
     * Incrementa o streak atual de um objetivo
     *
     * @param goalId ID do objetivo
     * @return Objetivo atualizado
     */
    public Goal incrementStreak(Long goalId) {
        Goal goal = findByIdOrThrow(goalId);
        goal.setCurrentStreak(goal.getCurrentStreak() + 1);
        return update(goal);
    }

    /**
     * Reseta o streak de um objetivo
     *
     * @param goalId ID do objetivo
     * @return Objetivo atualizado
     */
    public Goal resetStreak(Long goalId) {
        Goal goal = findByIdOrThrow(goalId);
        goal.setCurrentStreak(0);
        return update(goal);
    }

    /**
     * Atualiza o streak de um objetivo
     *
     * @param goalId    ID do objetivo
     * @param newStreak Novo valor do streak
     * @return Objetivo atualizado
     */
    public Goal updateStreak(Long goalId, Integer newStreak) {
        Goal goal = findByIdOrThrow(goalId);
        goal.setCurrentStreak(newStreak);
        return update(goal);
    }

    /**
     * Verifica se um objetivo está ativo
     *
     * @param goal Objetivo a verificar
     * @return true se ativo, false se expirado
     */
    public boolean isGoalActive(Goal goal) {
        if (goal.getEndDate() == null) {
            return true;
        }
        return !goal.getEndDate().isBefore(LocalDate.now());
    }

    /**
     * Busca objetivos que estão perto de expirar (próximos X dias)
     *
     * @param userId    ID do usuário
     * @param daysAhead Quantos dias à frente verificar
     * @return Lista de objetivos próximos de expirar
     */
    @Transactional(readOnly = true)
    public List<Goal> findGoalsExpiringSoon(Long userId, int daysAhead) {
        LocalDate limitDate = LocalDate.now().plusDays(daysAhead);
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId " +
                "AND g.endDate IS NOT NULL " +
                "AND g.endDate >= :today " +
                "AND g.endDate <= :limitDate " +
                "ORDER BY g.endDate ASC";
        return executeQuery(jpql, "userId", userId, "today", LocalDate.now(), "limitDate", limitDate);
    }

    /**
     * Conta quantos objetivos ativos um usuário possui
     *
     * @param userId ID do usuário
     * @return Número de objetivos ativos
     */
    @Transactional(readOnly = true)
    public long countActiveGoalsByUserId(Long userId) {
        String jpql = "SELECT COUNT(g) FROM Goal g WHERE g.userId = :userId " +
                "AND (g.endDate IS NULL OR g.endDate >= :today)";
        TypedQuery<Long> query = getEntityManager().createQuery(jpql, Long.class);
        query.setParameter("userId", userId);
        query.setParameter("today", LocalDate.now());
        return query.getSingleResult();
    }

    /**
     * Busca objetivos por unidade de medida
     *
     * @param userId    ID do usuário
     * @param valueUnit Unidade de medida
     * @return Lista de objetivos com a unidade especificada
     */
    @Transactional(readOnly = true)
    public List<Goal> findByUserIdAndValueUnit(Long userId, String valueUnit) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId " +
                "AND g.valueUnit = :valueUnit " +
                "ORDER BY g.id DESC";
        return executeQuery(jpql, "userId", userId, "valueUnit", valueUnit);
    }

    /**
     * Atualiza a data de início de um objetivo
     *
     * @param goalId       ID do objetivo
     * @param newStartDate Nova data de início
     * @return Objetivo atualizado
     */
    public Goal updateStartDate(Long goalId, LocalDate newStartDate) {
        Goal goal = findByIdOrThrow(goalId);
        goal.setStartDate(newStartDate);
        return update(goal);
    }

    /**
     * Atualiza a data final de um objetivo
     *
     * @param goalId     ID do objetivo
     * @param newEndDate Nova data final
     * @return Objetivo atualizado
     */
    public Goal updateEndDate(Long goalId, LocalDate newEndDate) {
        Goal goal = findByIdOrThrow(goalId);
        goal.setEndDate(newEndDate);
        return update(goal);
    }

    /**
     * Remove a data final de um objetivo (torna-o indefinido)
     *
     * @param goalId ID do objetivo
     * @return Objetivo atualizado
     */
    public Goal removeEndDate(Long goalId) {
        Goal goal = findByIdOrThrow(goalId);
        goal.setEndDate(null);
        return update(goal);
    }

    /**
     * Busca objetivos com streak maior ou igual a um valor
     *
     * @param userId    ID do usuário
     * @param minStreak Streak mínimo
     * @return Lista de objetivos com streak alto
     */
    @Transactional(readOnly = true)
    public List<Goal> findGoalsWithHighStreak(Long userId, Integer minStreak) {
        String jpql = "SELECT g FROM Goal g WHERE g.userId = :userId " +
                "AND g.currentStreak >= :minStreak " +
                "ORDER BY g.currentStreak DESC";
        return executeQuery(jpql, "userId", userId, "minStreak", minStreak);
    }

    /**
     * Calcula a duração de um objetivo em dias
     *
     * @param goal Objetivo
     * @return Número de dias desde o início (ou até o fim se expirado)
     */
    public long calculateGoalDuration(Goal goal) {
        LocalDate endDate = goal.getEndDate() != null ? goal.getEndDate() : LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(goal.getStartDate(), endDate);
    }

    /**
     * Remove todos os objetivos de um usuário
     *
     * @param userId ID do usuário
     * @return Número de objetivos removidos
     */
    public int deleteAllByUserId(Long userId) {
        List<Goal> goals = findByUserId(userId);
        goals.forEach(this::delete);
        return goals.size();
    }

    /**
     * Verifica se já existe um objetivo com o mesmo label para o usuário
     *
     * @param userId ID do usuário
     * @param label  Label do objetivo
     * @return true se existe, false caso contrário
     */
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndLabel(Long userId, String label) {
        return findByUserIdAndLabel(userId, label).isPresent();
    }
}