package progressofit.service;

import progressofit.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService extends GenericCrudService<User, Long> {

    /**
     * Busca usuário por email
     * @param email Email do usuário
     * @return Usuário encontrado ou null
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        String jpql = "SELECT u FROM User u WHERE u.email = :email";
        List<User> users = executeQuery(jpql, "email", email);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Busca usuários por nome (busca parcial, case insensitive)
     * @param name Nome ou parte do nome
     * @return Lista de usuários encontrados
     */
    @Transactional(readOnly = true)
    public List<User> findByNameContaining(String name) {
        String jpql = "SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(:name)";
        return executeQuery(jpql, "name", "%" + name + "%");
    }

    /**
     * Verifica se já existe um usuário com o email informado
     * @param email Email a ser verificado
     * @return true se o email já está em uso
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.email = :email";
        Long count = getEntityManager()
                .createQuery(jpql, Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    /**
     * Atualiza apenas o nome do usuário
     * @param id ID do usuário
     * @param newName Novo nome
     * @return Usuário atualizado ou null se não encontrado
     */
    public User updateUserName(Long id, String newName) {
        User user = findByIdOrThrow(id);
        user.setName(newName);
        return update(user);
    }

    /**
     * Atualiza apenas o email do usuário (se não estiver em uso)
     * @param id ID do usuário
     * @param newEmail Novo email
     * @return Usuário atualizado
     * @throws RuntimeException se o email já estiver em uso
     */
    public User updateUserEmail(Long id, String newEmail) {
        User user = findByIdOrThrow(id);

        // Verifica se o email não está sendo usado por outro usuário
        User existingUser = findByEmail(newEmail);
        if (existingUser != null && !existingUser.getId().equals(id)) {
            throw new RuntimeException("Email já está em uso por outro usuário");
        }

        user.setEmail(newEmail);
        return update(user);
    }

    /**
     * Atualiza a imagem de perfil do usuário
     * @param id ID do usuário
     * @param profileImgName Nome do arquivo da imagem de perfil
     * @return Usuário atualizado
     */
    @Transactional
    public User updateProfileImage(Long id, String profileImgName) {
        User user = findByIdOrThrow(id);
        user.setProfileImgName(profileImgName);
        return update(user);
    }

    /**
     * Lista usuários ordenados por nome
     * @return Lista de usuários ordenada por nome
     */
    @Transactional(readOnly = true)
    public List<User> findAllOrderByName() {
        String jpql = "SELECT u FROM User u ORDER BY u.name ASC";
        return executeQuery(jpql);
    }

    /**
     * Busca usuários que possuem imagem de perfil
     * @return Lista de usuários com imagem de perfil
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithProfileImage() {
        String jpql = "SELECT u FROM User u WHERE u.profileImgName IS NOT NULL";
        return executeQuery(jpql);
    }

    /**
     * Remove a imagem de perfil do usuário
     * @param id ID do usuário
     * @return Usuário atualizado
     */
    @Transactional
    public User removeProfileImage(Long id) {
        User user = findByIdOrThrow(id);
        user.setProfileImgName(null);
        return update(user);
    }
}