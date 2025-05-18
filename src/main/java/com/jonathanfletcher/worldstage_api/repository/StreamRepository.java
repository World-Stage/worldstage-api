package com.jonathanfletcher.worldstage_api.repository;

import com.jonathanfletcher.worldstage_api.model.entity.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StreamRepository extends JpaRepository<Stream, String> {
    Optional<Stream> findByActiveTrue();

    Optional<Stream> findByStreamKeyAndStatusNot(String streamKey, String status);
}
