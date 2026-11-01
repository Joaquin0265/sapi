package com.panificadora.sapi.seguridad.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers("/configurar-umbral").hasAuthority("Administrador")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .defaultSuccessUrl("/catalogo", true)
                        .permitAll())
                .logout(logout -> logout.permitAll());
        return http.build();
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService(PasswordEncoder encoder) {
        // Creamos un usuario Administrador
        org.springframework.security.core.userdetails.UserDetails admin = org.springframework.security.core.userdetails.User.builder()
            .username("admin")
            .password(encoder.encode("admin123"))
            .authorities("Administrador")
            .build();
            
        // Creamos un usuario Operario
        org.springframework.security.core.userdetails.UserDetails operario = org.springframework.security.core.userdetails.User.builder()
            .username("operario")
            .password(encoder.encode("operario123"))
            .authorities("OperarioAlmacen")
            .build();
            
        return new org.springframework.security.provisioning.InMemoryUserDetailsManager(admin, operario);
    }
}