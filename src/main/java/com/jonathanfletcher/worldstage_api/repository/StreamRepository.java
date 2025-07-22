package com.jonathanfletcher.worldstage_api.repository;

import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import com.jonathanfletcher.worldstage_api.model.entity.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreamRepository extends JpaRepository<Stream, UUID> {
    Optional<Stream> findByActiveTrue();

    Optional<Stream> findByStreamKeyAndStatusNot(UUID streamKey, StreamStatus status);

    Optional<Stream> findByStreamKeyAndActiveTrue(UUID streamKey);
}
