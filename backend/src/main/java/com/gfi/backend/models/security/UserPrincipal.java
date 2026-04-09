package com.gfi.backend.models.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {
    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final Long roleId;
    private final String roleName;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String password, String fullName, Long roleId, String roleName,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.roleId = roleId;
        this.roleName = roleName;
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
