package com.huyho.taskflow.auth;

import java.sql.SQLException;
import com.huyho.taskflow.user.User;
import com.huyho.taskflow.user.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class RegistrationServiceTests {
    @Test
    void translatesConcurrentUniqueEmailViolationButNotOtherDatabaseFailures() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        when(passwords.encode(any())).thenReturn("encoded");
        RegistrationService service = new RegistrationService(users, passwords);
        RegisterRequest request = new RegisterRequest("race@example.com", "Learner", "Example-password-123!");
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("Conflict",
                new ConstraintViolationException("Conflict", new SQLException(), "users_email_unique")));
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(EmailAlreadyRegisteredException.class);
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("Other",
                new ConstraintViolationException("Other", new SQLException(), "users_display_name_not_blank")));
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
