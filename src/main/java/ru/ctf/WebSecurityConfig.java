package ru.ctf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ComponentScan
public class WebSecurityConfig {

    @Value("${security.basic.username}")
    private String username;

    @Value("${security.basic.password-hash}")
    private String passwordHash;

    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername(username)
                .password(passwordHash)
                .roles("USER")
                .build());
        return manager;
    }

    @Bean
    public SecurityFilterChain apiGetFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/get/*")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest()
                        .authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public SecurityFilterChain apiPutFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/put")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest()
                        .authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

}