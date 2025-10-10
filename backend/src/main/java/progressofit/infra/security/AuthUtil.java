package progressofit.infra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import progressofit.model.user.User;
import progressofit.service.UserService;

@Component
public class AuthUtil {

    @Autowired
    private UserService userService;

    /**
     * Obtém o usuário atualmente autenticado
     * @return User autenticado ou null se não autenticado
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userService.findByEmail(userDetails.getUsername());
        }
        return null;
    }

    /**
     * Obtém o ID do usuário atualmente autenticado
     * @return ID do usuário ou null se não autenticado
     */
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Obtém o email do usuário atualmente autenticado
     * @return email do usuário ou null se não autenticado
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return null;
    }

    /**
     * Verifica se o usuário atual pode acessar um recurso específico
     * @param resourceUserId ID do usuário dono do recurso
     * @return true se pode acessar, false caso contrário
     */
    public boolean canAccessResource(Long resourceUserId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Admin pode acessar qualquer recurso
        if (currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // Usuário comum só pode acessar seus próprios recursos
        return currentUser.getId().equals(resourceUserId);
    }
}