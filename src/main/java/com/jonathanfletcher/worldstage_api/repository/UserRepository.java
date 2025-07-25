package com.jonathanfletcher.worldstage_api.repository;

import com.jonathanfletcher.worldstage_api.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByStreamKey(UUID streamKey);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
