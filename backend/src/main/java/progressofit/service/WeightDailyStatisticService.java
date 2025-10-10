package progressofit.service;

import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progressofit.model.weightdata.WeightDailyStatistic;
import progressofit.model.weightdata.dto.WeeklyWeightDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WeightDailyStatisticService extends GenericCrudService<WeightDailyStatistic, Long> {

    /**
     * Busca todas as estatísticas de peso de um usuário
     *
     * @param userId ID do usuário
     * @return Lista de estatísticas de peso do usuário
     */
    @Transactional(readOnly = true)
    public List<WeightDailyStatistic> findByUserId(Long userId) {
        String jpql = "SELECT w FROM WeightDailyStatistic w WHERE w.userId = :userId ORDER BY w.date DESC";
        return executeQuery(jpql, "userId", userId);
    }

    /**
     * Busca estatísticas de peso de um usuário em um período específico
     *
     * @param userId    ID do usuário
     * @param startDate Data inicial (inclusive)
     * @param endDate   Data final (inclusive)
     * @return Lista de estatísticas de peso no período
     */
    @Transactional(readOnly = true)
    public List<WeightDailyStatistic> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate) {
        String jpql = "SELECT w FROM WeightDailyStatistic w " +
                "WHERE w.userId = :userId " +
                "AND w.date >= :startDate " +
                "AND w.date <= :endDate " +
                "ORDER BY w.date ASC";
        return executeQuery(jpql, "userId", userId, "startDate", startDate, "endDate", endDate);
    }

    /**
     * Busca estatística de peso específica de um usuário em uma data
     *
     * @param userId ID do usuário
     * @param date   Data específica
     * @return Optional contendo a estatística de peso ou vazio se não encontrada
     */
    @Transactional(readOnly = true)
    public Optional<WeightDailyStatistic> findByUserIdAndDate(Long userId, LocalDate date) {
        try {
            String jpql = "SELECT w FROM WeightDailyStatistic w WHERE w.userId = :userId AND w.date = :date";
            TypedQuery<WeightDailyStatistic> query = getEntityManager().createQuery(jpql, WeightDailyStatistic.class);
            query.setParameter("userId", userId);
            query.setParameter("date", date);

            List<WeightDailyStatistic> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar estatística de peso por usuário e data: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza ou cria uma estatística de peso para um usuário e data específica (upsert)
     *
     * @param statistic Estatística de peso a ser salva/atualizada
     * @return Estatística de peso processada
     */
    @Transactional
    public WeightDailyStatistic upsertByUserIdAndDate(WeightDailyStatistic statistic) {
        try {
            Optional<WeightDailyStatistic> existing = findByUserIdAndDate(
                    statistic.getUserId(),
                    statistic.getDate()
            );

            if (existing.isPresent()) {
                // Atualiza registro existente
                WeightDailyStatistic existingStatistic = existing.get();
                existingStatistic.setWeightKg(statistic.getWeightKg());
                return update(existingStatistic);
            } else {
                // Cria novo registro
                return save(statistic);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer upsert da estatística de peso: " + e.getMessage(), e);
        }
    }

    /**
     * Remove uma estatística de peso específica de um usuário em uma data
     *
     * @param userId ID do usuário
     * @param date   Data específica
     * @return true se removido com sucesso, false se não encontrado
     */
    @Transactional
    public boolean deleteByUserIdAndDate(Long userId, LocalDate date) {
        try {
            Optional<WeightDailyStatistic> statistic = findByUserIdAndDate(userId, date);
            if (statistic.isPresent()) {
                delete(statistic.get());
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar estatística de peso por usuário e data: " + e.getMessage(), e);
        }
    }

    /**
     * Remove todas as estatísticas de peso de um usuário
     *
     * @param userId ID do usuário
     * @return Número de registros removidos
     */
    @Transactional
    public int deleteByUserId(Long userId) {
        try {
            String jpql = "DELETE FROM WeightDailyStatistic w WHERE w.userId = :userId";
            return getEntityManager().createQuery(jpql)
                    .setParameter("userId", userId)
                    .executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar estatísticas de peso por usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se existe estatística de peso para um usuário em uma data específica
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
     * Conta o total de estatísticas de peso de um usuário
     *
     * @param userId ID do usuário
     * @return Número total de estatísticas de peso do usuário
     */
    @Transactional(readOnly = true)
    public long countByUserId(Long userId) {
        try {
            String jpql = "SELECT COUNT(w) FROM WeightDailyStatistic w WHERE w.userId = :userId";
            return getEntityManager().createQuery(jpql, Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar estatísticas de peso por usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Busca as últimas N estatísticas de peso de um usuário
     *
     * @param userId ID do usuário
     * @param limit  Número de registros a retornar
     * @return Lista das últimas estatísticas de peso do usuário
     */
    @Transactional(readOnly = true)
    public List<WeightDailyStatistic> findLastNByUserId(Long userId, int limit) {
        try {
            String jpql = "SELECT w FROM WeightDailyStatistic w " +
                    "WHERE w.userId = :userId " +
                    "ORDER BY w.date DESC";
            return getEntityManager().createQuery(jpql, WeightDailyStatistic.class)
                    .setParameter("userId", userId)
                    .setMaxResults(limit)
                    .getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar últimas estatísticas de peso: " + e.getMessage(), e);
        }
    }

    /**
     * Busca estatísticas de peso de um usuário no mês atual
     *
     * @param userId ID do usuário
     * @return Lista de estatísticas de peso do mês atual
     */
    @Transactional(readOnly = true)
    public List<WeightDailyStatistic> findByUserIdCurrentMonth(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        return findByUserIdAndDateBetween(userId, startOfMonth, endOfMonth);
    }

    /**
     * Busca estatísticas de peso de um usuário na semana atual
     *
     * @param userId ID do usuário
     * @return Lista de estatísticas de peso da semana atual
     */
    @Transactional(readOnly = true)
    public List<WeightDailyStatistic> findByUserIdCurrentWeek(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return findByUserIdAndDateBetween(userId, startOfWeek, endOfWeek);
    }

    /**
     * Busca estatísticas semanais de peso para um usuário em um período específico
     * Retorna todas as semanas do período, mesmo aquelas sem registros (com peso 0)
     *
     * @param userId    ID do usuário
     * @param startDate Data inicial do período
     * @param endDate   Data final do período
     * @return Lista com as estatísticas de peso por semana
     */
    @Transactional(readOnly = true)
    public List<WeeklyWeightDTO> findWeeklyWeightStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<WeightDailyStatistic> statistics = findByUserIdAndDateBetween(userId, startDate, endDate);

            Map<String, List<BigDecimal>> weeklyWeights = new LinkedHashMap<>();

            LocalDate weekStart = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1);
            LocalDate weekEnd = endDate.minusDays(endDate.getDayOfWeek().getValue() - 1).plusDays(6);

            LocalDate currentWeekStart = weekStart;
            while (!currentWeekStart.isAfter(weekEnd)) {
                weeklyWeights.put(currentWeekStart.toString(), new java.util.ArrayList<>());
                currentWeekStart = currentWeekStart.plusDays(7);
            }

            for (WeightDailyStatistic stat : statistics) {
                LocalDate statWeekStart = stat.getDate().minusDays(stat.getDate().getDayOfWeek().getValue() - 1);
                String weekKey = statWeekStart.toString();

                if (weeklyWeights.containsKey(weekKey)) {
                    weeklyWeights.get(weekKey).add(stat.getWeightKg());
                }
            }

            return weeklyWeights.entrySet().stream()
                    .map(entry -> {
                        List<BigDecimal> weights = entry.getValue();
                        LocalDate weekStartDate = LocalDate.parse(entry.getKey());

                        if (weights.isEmpty()) {
                            return new WeeklyWeightDTO(
                                    weekStartDate,
                                    weekStartDate.plusDays(6),
                                    BigDecimal.ZERO,
                                    BigDecimal.ZERO,
                                    BigDecimal.ZERO,
                                    0
                            );
                        } else {
                            BigDecimal sum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                            BigDecimal average = sum.divide(BigDecimal.valueOf(weights.size()), 2, RoundingMode.HALF_UP);
                            BigDecimal min = weights.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                            BigDecimal max = weights.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

                            return new WeeklyWeightDTO(
                                    weekStartDate,
                                    weekStartDate.plusDays(6),
                                    average,
                                    min,
                                    max,
                                    weights.size()
                            );
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar estatísticas semanais de peso: " + e.getMessage(), e);
        }
    }

    /**
     * Busca as estatísticas semanais de peso para um usuário nos últimos N meses
     *
     * @param userId     ID do usuário
     * @param monthsBack Número de meses para trás (ex: 3 = últimos 3 meses)
     * @return Lista com as estatísticas de peso por semana
     */
    @Transactional(readOnly = true)
    public List<WeeklyWeightDTO> findWeeklyWeightStatisticsLastMonths(Long userId, int monthsBack) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(monthsBack).withDayOfMonth(1);
        return findWeeklyWeightStatistics(userId, startDate, endDate);
    }

    /**
     * Busca as estatísticas semanais de peso para um usuário no ano atual
     *
     * @param userId ID do usuário
     * @return Lista com as estatísticas de peso por semana
     */
    @Transactional(readOnly = true)
    public List<WeeklyWeightDTO> findWeeklyWeightStatisticsCurrentYear(Long userId) {
        LocalDate startOfYear = LocalDate.now().withDayOfYear(1);
        LocalDate endOfYear = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
        return findWeeklyWeightStatistics(userId, startOfYear, endOfYear);
    }

    /**
     * Busca o peso mais recente de um usuário
     *
     * @param userId ID do usuário
     * @return Optional contendo o peso mais recente ou vazio se não encontrado
     */
    @Transactional(readOnly = true)
    public Optional<WeightDailyStatistic> findLatestWeightByUserId(Long userId) {
        try {
            String jpql = "SELECT w FROM WeightDailyStatistic w " +
                    "WHERE w.userId = :userId " +
                    "ORDER BY w.date DESC";
            List<WeightDailyStatistic> results = getEntityManager()
                    .createQuery(jpql, WeightDailyStatistic.class)
                    .setParameter("userId", userId)
                    .setMaxResults(1)
                    .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar peso mais recente: " + e.getMessage(), e);
        }
    }

    /**
     * Busca o peso médio de um usuário em um período
     *
     * @param userId    ID do usuário
     * @param startDate Data inicial
     * @param endDate   Data final
     * @return Peso médio no período
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> findAverageWeightByPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            String jpql = "SELECT AVG(w.weightKg) FROM WeightDailyStatistic w " +
                    "WHERE w.userId = :userId " +
                    "AND w.date >= :startDate " +
                    "AND w.date <= :endDate";
            BigDecimal average = getEntityManager()
                    .createQuery(jpql, BigDecimal.class)
                    .setParameter("userId", userId)
                    .setParameter("startDate", startDate)
                    .setParameter("endDate", endDate)
                    .getSingleResult();
            return average != null ? Optional.of(average.setScale(2, RoundingMode.HALF_UP)) : Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular peso médio: " + e.getMessage(), e);
        }
    }
}