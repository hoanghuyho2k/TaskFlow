package com.huyho.taskflow.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Callers supply the normalized email (including during future login).
    Optional<User> findByEmail(String email);
}
