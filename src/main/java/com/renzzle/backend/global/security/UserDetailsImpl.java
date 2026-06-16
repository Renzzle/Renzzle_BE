package com.renzzle.backend.global.security;

import com.renzzle.backend.domain.user.domain.UserEntity;
import lombok.Getter;

import java.io.Serial;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final transient UserEntity user;
    private final String password;
    private final List<String> authorities;

    public UserDetailsImpl(UserEntity user, String password, List<String> authorities) {
        this.user = user;
        this.password = password;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.authorities == null) {
            return Collections.emptyList();
        }
        return this.authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

}
