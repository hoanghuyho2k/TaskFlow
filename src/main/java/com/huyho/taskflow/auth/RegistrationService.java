package com.huyho.taskflow.auth;

import com.huyho.taskflow.user.User;
import com.huyho.taskflow.user.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public RegistrationService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (users.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(email, request.displayName(), passwords.encode(request.password()));
        try {
            // Flush here so a concurrent email conflict can be translated before commit.
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "users_email_unique".equals(violation.getConstraintName())) {
                    throw new EmailAlreadyRegisteredException();
                }
            }
            throw exception;
        }
    }
}
