package progressofit.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

/**
 * Classe genérica para operações CRUD com abstração do gerenciamento de sessões
 * Utiliza EntityManager do Spring Data JPA com Hibernate
 *
 * @param <T> Tipo da entidade
 * @param <ID> Tipo do identificador da entidade
 */
@Service
@Transactional
public abstract class GenericCrudService<T, ID extends Serializable> {

    @PersistenceContext
    private EntityManager entityManager;

    private final Class<T> entityClass;

    @SuppressWarnings("unchecked")
    public GenericCrudService() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    /**
     * Salva uma nova entidade
     * @param entity Entidade a ser salva
     * @return Entidade salva com ID gerado
     */
    public T save(T entity) {
        try {
            entityManager.persist(entity);
            entityManager.flush();
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar entidade: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza uma entidade existente
     * @param entity Entidade a ser atualizada
     * @return Entidade atualizada
     */
    public T update(T entity) {
        try {
            T merged = entityManager.merge(entity);
            entityManager.flush();
            return merged;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar entidade: " + e.getMessage(), e);
        }
    }

    /**
     * Salva ou atualiza uma entidade (upsert)
     * @param entity Entidade a ser salva/atualizada
     * @return Entidade processada
     */
    public T saveOrUpdate(T entity) {
        try {
            T merged = entityManager.merge(entity);
            entityManager.flush();
            return merged;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar/atualizar entidade: " + e.getMessage(), e);
        }
    }

    /**
     * Busca uma entidade pelo ID
     * @param id ID da entidade
     * @return Optional contendo a entidade ou vazio se não encontrada
     */
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        try {
            T entity = entityManager.find(entityClass, id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar entidade por ID: " + e.getMessage(), e);
        }
    }

    /**
     * Busca uma entidade pelo ID, lança exceção se não encontrada
     * @param id ID da entidade
     * @return Entidade encontrada
     * @throws EntityNotFoundException se a entidade não for encontrada
     */
    @Transactional(readOnly = true)
    public T findByIdOrThrow(ID id) {
        return findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Entidade " + entityClass.getSimpleName() + " com ID " + id + " não encontrada"));
    }

    /**
     * Lista todas as entidades
     * @return Lista de todas as entidades
     */
    @Transactional(readOnly = true)
    public List<T> findAll() {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);
            cq.select(root);

            TypedQuery<T> query = entityManager.createQuery(cq);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar todas as entidades: " + e.getMessage(), e);
        }
    }

    /**
     * Lista entidades com paginação
     * @param page Número da página (começando em 0)
     * @param size Tamanho da página
     * @return Lista paginada de entidades
     */
    @Transactional(readOnly = true)
    public List<T> findAll(int page, int size) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);
            cq.select(root);

            TypedQuery<T> query = entityManager.createQuery(cq);
            query.setFirstResult(page * size);
            query.setMaxResults(size);

            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar entidades paginadas: " + e.getMessage(), e);
        }
    }

    /**
     * Conta o total de entidades
     * @return Número total de entidades
     */
    @Transactional(readOnly = true)
    public long count() {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<T> root = cq.from(entityClass);
            cq.select(cb.count(root));

            TypedQuery<Long> query = entityManager.createQuery(cq);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar entidades: " + e.getMessage(), e);
        }
    }

    /**
     * Remove uma entidade pelo ID
     * @param id ID da entidade a ser removida
     * @return true se a entidade foi removida, false se não foi encontrada
     */
    public boolean deleteById(ID id) {
        try {
            T entity = entityManager.find(entityClass, id);
            if (entity != null) {
                entityManager.remove(entity);
                entityManager.flush();
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar entidade por ID: " + e.getMessage(), e);
        }
    }

    /**
     * Remove uma entidade
     * @param entity Entidade a ser removida
     */
    public void delete(T entity) {
        try {
            if (entityManager.contains(entity)) {
                entityManager.remove(entity);
            } else {
                // Se a entidade não está no contexto atual, faz merge primeiro
                T managedEntity = entityManager.merge(entity);
                entityManager.remove(managedEntity);
            }
            entityManager.flush();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar entidade: " + e.getMessage(), e);
        }
    }

    /**
     * Remove todas as entidades (cuidado!)
     */
    public void deleteAll() {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);
            cq.select(root);

            List<T> entities = entityManager.createQuery(cq).getResultList();
            for (T entity : entities) {
                entityManager.remove(entity);
            }
            entityManager.flush();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar todas as entidades: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se uma entidade existe pelo ID
     * @param id ID da entidade
     * @return true se existe, false caso contrário
     */
    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        try {
            T entity = entityManager.find(entityClass, id);
            return entity != null;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar existência da entidade: " + e.getMessage(), e);
        }
    }

    /**
     * Executa uma query JPQL personalizada
     * @param jpql Query JPQL
     * @return Lista de entidades resultado da query
     */
    @Transactional(readOnly = true)
    public List<T> executeQuery(String jpql) {
        try {
            TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar query personalizada: " + e.getMessage(), e);
        }
    }

    /**
     * Executa uma query JPQL personalizada com parâmetros
     * @param jpql Query JPQL
     * @param parameters Array de parâmetros [nome1, valor1, nome2, valor2, ...]
     * @return Lista de entidades resultado da query
     */
    @Transactional(readOnly = true)
    public List<T> executeQuery(String jpql, Object... parameters) {
        try {
            TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);

            // Define os parâmetros em pares (nome, valor)
            for (int i = 0; i < parameters.length; i += 2) {
                query.setParameter((String) parameters[i], parameters[i + 1]);
            }

            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar query personalizada com parâmetros: " + e.getMessage(), e);
        }
    }

    /**
     * Força a sincronização das mudanças com o banco de dados
     */
    public void flush() {
        entityManager.flush();
    }

    /**
     * Limpa o contexto de persistência
     */
    public void clear() {
        entityManager.clear();
    }

    /**
     * Atualiza uma entidade do contexto de persistência com dados do banco
     * @param entity Entidade a ser atualizada
     */
    public void refresh(T entity) {
        try {
            entityManager.refresh(entity);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer refresh da entidade: " + e.getMessage(), e);
        }
    }

    /**
     * Desanexa uma entidade do contexto de persistência
     * @param entity Entidade a ser desanexada
     */
    public void detach(T entity) {
        entityManager.detach(entity);
    }

    /**
     * Obtém o EntityManager para operações avançadas
     * Use com cuidado!
     * @return EntityManager atual
     */
    protected EntityManager getEntityManager() {
        return entityManager;
    }
}