package progressofit.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import progressofit.infra.security.AuthUtil;
import progressofit.model.user.User;
import progressofit.model.user.dto.UserDTO;
import progressofit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<UserDTO> getUser() {
        try {
            Long userId = authUtil.getCurrentUserId();
            Optional<User> user = userService.findById(userId);
            if(user.isPresent()){
                User currentUser = user.get();
                UserDTO userDTO = new UserDTO(
                        currentUser.getName(),
                        currentUser.getEmail(),
                        "",
                        currentUser.getProfileImgName()
                );
                return ResponseEntity.ok(userDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.findByEmail(authentication.name).id == #id")
    public ResponseEntity<User> buscarPorId(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User updatedUserData) {
        try {
            long currentUserId = authUtil.getCurrentUserId();
            Optional<User> existingUserOptional = this.userService.findById(currentUserId);

            if (existingUserOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User existingUser = existingUserOptional.get();

            if (hasEmailConflict(updatedUserData.getEmail(), existingUser.getEmail(), currentUserId)) {
                return ResponseEntity.badRequest().build();
            }

            updateUserFields(updatedUserData, existingUser);

            User updatedUser = userService.update(existingUser);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean hasEmailConflict(String newEmail, String currentEmail, long userId) {
        if (newEmail == null || newEmail.equals(currentEmail)) {
            return false;
        }

        User userWithSameEmail = this.userService.findByEmail(newEmail);
        return userWithSameEmail != null && !userWithSameEmail.getId().equals(userId);
    }

    private void updateUserFields(User updatedUserData, User existingUser) {
        if (updatedUserData.getName() != null) {
            existingUser.setName(updatedUserData.getName());
        }
        if (updatedUserData.getEmail() != null) {
            existingUser.setEmail(updatedUserData.getEmail());
        }
        if (updatedUserData.getPassword() != null) {
            existingUser.setPassword(this.passwordEncoder.encode(updatedUserData.getPassword()));
        }
        if (updatedUserData.getProfileImgName() != null) {
            existingUser.setProfileImgName(updatedUserData.getProfileImgName());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.findByEmail(authentication.name).id == #id")
    public ResponseEntity<User> atualizarUsuario(@PathVariable Long id, @RequestBody User user) {
        try {
            Optional<User> originalUser = userService.findById(id);
            if (!originalUser.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            User existingUser = userService.findByEmail(user.getEmail());
            if (existingUser != null && !existingUser.getId().equals(id)) {
                return ResponseEntity.badRequest().build();
            }

            user.setId(id);
            // Preserva a imagem de perfil se não for enviada uma nova
            if (user.getProfileImgName() == null) {
                originalUser.ifPresent(value -> user.setProfileImgName(value.getProfileImgName()));
            }

            User userAtualizado = userService.update(user);
            return ResponseEntity.ok(userAtualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/nome")
    @PreAuthorize("hasRole('ADMIN') or @userService.findByEmail(authentication.name).id == #id")
    public ResponseEntity<User> atualizarNome(@PathVariable Long id, @RequestBody String novoNome) {
        try {
            User userAtualizado = userService.updateUserName(id, novoNome);
            return ResponseEntity.ok(userAtualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/email")
    @PreAuthorize("hasRole('ADMIN') or @userService.findByEmail(authentication.name).id == #id")
    public ResponseEntity<User> atualizarEmail(@PathVariable Long id, @RequestBody String novoEmail) {
        try {
            User userAtualizado = userService.updateUserEmail(id, novoEmail);
            return ResponseEntity.ok(userAtualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/profile-image")
    @PreAuthorize("hasRole('ADMIN') or @userService.findByEmail(authentication.name).id == #id")
    public ResponseEntity<User> atualizarImagemPerfil(@PathVariable Long id, @RequestBody String profileImgName) {
        try {
            User userAtualizado = userService.updateProfileImage(id, profileImgName);
            return ResponseEntity.ok(userAtualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.findByEmail(authentication.name).id == #id")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        try {
            if (userService.deleteById(id)) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<User> users = userService.findAll(page, size);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> contarUsuarios() {
        long count = userService.count();
        return ResponseEntity.ok(count);
    }
}