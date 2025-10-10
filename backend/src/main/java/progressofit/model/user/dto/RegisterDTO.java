package progressofit.model.user.dto;

import progressofit.model.user.UserRole;

public record RegisterDTO(String name, String email, String password, UserRole role) {
}
