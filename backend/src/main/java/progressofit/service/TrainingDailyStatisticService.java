package progressofit.service;

import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progressofit.model.trainingdata.TrainingDailyStatistic;
import progressofit.model.trainingdata.dto.WeeklyTrainingCountDTO;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TrainingDailyStatisticService extends GenericCrudService<TrainingDailyStatistic, Long> {

    /**
     * Busca todas as estatísticas de um usuário
     *
     * @param userId ID do usuário
     * @return Lista de estatísticas do usuário
     */
    @Transactional(readOnly = true)
    public List<TrainingDailyStatistic> findByUserId(Long userId) {
        String jpql = "SELECT t FROM TrainingDailyStatistic t WHERE t.userId = :userId ORDER BY t.date DESC";
        return executeQuery(jpql, "userId", userId);
    }

    /**
     * Busca estatísticas de um usuário em um período específico
     *
     * @param userId    ID do usuário
     * @param startDate Data inicial (inclusive)
     * @param endDate   Data final (inclusive)
     * @return Lista de estatísticas no período
     */
    @Transactional(readOnly = true)
    public List<TrainingDailyStatistic> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate) {
        String jpql = "SELECT t FROM TrainingDailyStatistic t " +
                "WHERE t.userId = :userId " +
                "AND t.date >= :startDate " +
                "AND t.date <= :endDate " +
                "ORDER BY t.date ASC";
        return executeQuery(jpql, "userId", userId, "startDate", startDate, "endDate", endDate);
    }

    /**
     * Busca estatística específica de um usuário em uma data
     *
     * @param userId ID do usuário
     * @param date   Data específica
     * @return Optional contendo a estatística ou vazio se não encontrada
     */
    @Transactional(readOnly = true)
    public Optional<TrainingDailyStatistic> findByUserIdAndDate(Long userId, LocalDate date) {
        try {
            String jpql = "SELECT t FROM TrainingDailyStatistic t WHERE t.userId = :userId AND t.date = :date";
            TypedQuery<TrainingDailyStatistic> query = getEntityManager().createQuery(jpql, TrainingDailyStatistic.class);
            query.setParameter("userId", userId);
            query.setParameter("date", date);

            List<TrainingDailyStatistic> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar estatística por usuário e data: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza ou cria uma estatística para um usuário e data específica (upsert)
     *
     * @param statistic Estatística a ser salva/atualizada
     * @return Estatística processada
     */
    @Transactional
    public TrainingDailyStatistic upsertByUserIdAndDate(TrainingDailyStatistic statistic) {
        try {
            Optional<TrainingDailyStatistic> existing = findByUserIdAndDate(
                    statistic.getUserId(),
                    statistic.getDate()
            );

            if (existing.isPresent()) {
                // Atualiza registro existente
                TrainingDailyStatistic existingStatistic = existing.get();
                existingStatistic.setCount(statistic.getCount());
                return update(existingStatistic);
            } else {
                // Cria novo registro
                return save(statistic);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer upsert da estatística: " + e.getMessage(), e);
        }
    }

    /**
     * Remove uma estatística específica de um usuário em uma data
     *
     * @param userId ID do usuário
     * @param date   Data específica
     * @return true se removido com sucesso, false se não encontrado
     */
    @Transactional
    public boolean deleteByUserIdAndDate(Long userId, LocalDate date) {
        try {
            Optional<TrainingDailyStatistic> statistic = findByUserIdAndDate(userId, date);
            if (statistic.isPresent()) {
                delete(statistic.get());
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar estatística por usuário e data: " + e.getMessage(), e);
        }
    }

    /**
     * Remove todas as estatísticas de um usuário
     *
     * @param userId ID do usuário
     * @return Número de registros removidos
     */
    @Transactional
    public int deleteByUserId(Long userId) {
        try {
            String jpql = "DELETE FROM TrainingDailyStatistic t WHERE t.userId = :userId";
            return getEntityManager().createQuery(jpql)
                    .setParameter("userId", userId)
                    .executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar estatísticas por usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se existe estatística para um usuário em uma data específica
     *
     * @param userId ID do usuário
     * @param date   Data específica
     * @return true se existe, false caso contrário
     */
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndDate(Long userId, LocalDate date) {
        return findByUserIdAndDate(userId, date).isPresent();
    }

    /**
     * Conta o total de estatísticas de um usuário
     *
     * @param userId ID do usuário
     * @return Número total de estatísticas do usuário
     */
    @Transactional(readOnly = true)
    public long countByUserId(Long userId) {
        try {
            String jpql = "SELECT COUNT(t) FROM TrainingDailyStatistic t WHERE t.userId = :userId";
            return getEntityManager().createQuery(jpql, Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar estatísticas por usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Busca as últimas N estatísticas de um usuário
     *
     * @param userId ID do usuário
     * @param limit  Número de registros a retornar
     * @return Lista das últimas estatísticas do usuário
     */
    @Transactional(readOnly = true)
    public List<TrainingDailyStatistic> findLastNByUserId(Long userId, int limit) {
        try {
            String jpql = "SELECT t FROM TrainingDailyStatistic t " +
                    "WHERE t.userId = :userId " +
                    "ORDER BY t.date DESC";
            return getEntityManager().createQuery(jpql, TrainingDailyStatistic.class)
                    .setParameter("userId", userId)
                    .setMaxResults(limit)
                    .getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar últimas estatísticas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca estatísticas de um usuário no mês atual
     *
     * @param userId ID do usuário
     * @return Lista de estatísticas do mês atual
     */
    @Transactional(readOnly = true)
    public List<TrainingDailyStatistic> findByUserIdCurrentMonth(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        return findByUserIdAndDateBetween(userId, startOfMonth, endOfMonth);
    }

    /**
     * Busca estatísticas de um usuário na semana atual
     * @param userId ID do usuário
     * @return Lista de estatísticas da semana atual
     */
    @Transactional(readOnly = true)
    public List<TrainingDailyStatistic> findByUserIdCurrentWeek(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return findByUserIdAndDateBetween(userId, startOfWeek, endOfWeek);
    }

    /**
     * Busca a quantidade de treinos por semana para um usuário em um período específico
     *
     * @param userId    ID do usuário
     * @param startDate Data inicial do período
     * @param endDate   Data final do período
     * @return Lista com a quantidade de treinos por semana
     */
    @Transactional(readOnly = true)
    public List<WeeklyTrainingCountDTO> findWeeklyTrainingCounts(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<TrainingDailyStatistic> statistics = findByUserIdAndDateBetween(userId, startDate, endDate);

            Map<String, Integer> weeklyTotals = new LinkedHashMap<>();

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                LocalDate weekStart = currentDate.minusDays(currentDate.getDayOfWeek().getValue() - 1);
                String weekKey = weekStart.toString();
                weeklyTotals.putIfAbsent(weekKey, 0);

                LocalDate finalCurrentDate = currentDate;
                Optional<TrainingDailyStatistic> statForDate = statistics.stream()
                        .filter(stat -> stat.getDate().equals(finalCurrentDate))
                        .findFirst();

                if (statForDate.isPresent()) {
                    weeklyTotals.put(weekKey, weeklyTotals.get(weekKey) + statForDate.get().getCount());
                }

                currentDate = currentDate.plusDays(1);
            }

            return weeklyTotals.entrySet().stream()
                    .map(entry -> new WeeklyTrainingCountDTO(
                            LocalDate.parse(entry.getKey()),
                            LocalDate.parse(entry.getKey()).plusDays(6),
                            entry.getValue()
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar contagem semanal de treinos: " + e.getMessage(), e);
        }
    }

    /**
     * Busca a quantidade de treinos por semana para um usuário nos últimos N meses
     *
     * @param userId     ID do usuário
     * @param monthsBack Número de meses para trás (ex: 3 = últimos 3 meses)
     * @return Lista com a quantidade de treinos por semana
     */
    @Transactional(readOnly = true)
    public List<WeeklyTrainingCountDTO> findWeeklyTrainingCountsLastMonths(Long userId, int monthsBack) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(monthsBack).withDayOfMonth(1);
        return findWeeklyTrainingCounts(userId, startDate, endDate);
    }

    /**
     * Busca a quantidade de treinos por semana para um usuário no ano atual
     *
     * @param userId ID do usuário
     * @return Lista com a quantidade de treinos por semana
     */
    @Transactional(readOnly = true)
    public List<WeeklyTrainingCountDTO> findWeeklyTrainingCountsCurrentYear(Long userId) {
        LocalDate startOfYear = LocalDate.now().withDayOfYear(1);
        LocalDate endOfYear = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
        return findWeeklyTrainingCounts(userId, startOfYear, endOfYear);
    }
}