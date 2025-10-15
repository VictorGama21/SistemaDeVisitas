package com.inter.SistemaDeVisitas.security;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public JpaUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = repo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        RoleGroup group = u.getRoleGroup();
        if (group == null) {
            group = RoleGroup.LOJA;
            u.setRoleGroup(group);
            repo.save(u);
        }

        String role = "ROLE_" + group.name();
        
        return new org.springframework.security.core.userdetails.User(
                u.getEmail(),
                u.getPassword(),
                u.isEnabled(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
