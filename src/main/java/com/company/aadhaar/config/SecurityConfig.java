package com.company.aadhaar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;



@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .disable()
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/aadhaar/health", "/api/aadhaar/digilocker/authorize-url", "/api/digilocker/callback").permitAll()
                        .anyRequest().authenticated()
                )

                // Disables default form login pages.
                .formLogin(form -> form.disable())
                // Disable basic auth challenges too.
                .httpBasic(httpBasic -> httpBasic.disable())
                .build();
    }
}



