package com.jonathanfletcher.worldstage_api.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.jonathanfletcher.worldstage_api.spring.security.model.entity.RefreshToken;
import com.jonathanfletcher.worldstage_api.spring.security.model.entity.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder
@Entity
@Table(name = "users", schema = "edge",
        uniqueConstraints = {@UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class User implements UserDetails {

    @Id
    private UUID id;

    @NotBlank(message = "Username cannot be blank")
    @Size(max = 50, message = "UserName cannot be longer than 50 characters")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Size(max = 50, message = "Email cannot be longer than 50 characters")
    private String email;

    @NotBlank
    @Size(max = 120)
    @JsonIgnore
    @ToString.Exclude
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            schema = "edge",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private Collection<Role> roles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @CreationTimestamp
    private Instant createdTs;

    @UpdateTimestamp
    private Instant lastModifiedTs;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
