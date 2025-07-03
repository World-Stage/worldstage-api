package com.jonathanfletcher.worldstage_api.spring.security.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Collection;
import java.util.UUID;

@Builder
@Entity
@Table(name = "privileges", schema = "edge")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Privilege {
    @Id
    private UUID id;

    private String name;

    @ManyToMany(mappedBy = "privileges")
    @JsonIgnore
    private Collection<Role> roles;
}
