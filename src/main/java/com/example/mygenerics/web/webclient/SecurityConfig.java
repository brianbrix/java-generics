package com.example.mygenerics.web.webclient;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Arrays;
@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(prefix = "security", name = "required", havingValue = "true")
@Log4j2
public class
SecurityConfig {
    @Value("ROLES")
    private String[] roles;
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http) {
        log.info("ROLES: {}", Arrays.toString(roles));
        http.csrf().disable()
                .authorizeExchange()
                .pathMatchers("/**").hasAnyRole(roles)
                .and()
                .httpBasic();
        return http.build();
    }
}
