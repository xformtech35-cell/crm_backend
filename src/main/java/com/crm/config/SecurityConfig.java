package com.crm.config;

import com.crm.security.JwtAuthenticationFilter;
import com.crm.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomCorsFilter customCorsFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                        "/api/auth/**",
                        "/api/uploads/**",
                        "/api/leads",
                        "/api/leads/**",
                        "/api/opportunities",
                        "/api/opportunities/**",
                        "/api/calendar",
                        "/api/calendar/**",
                        "/api/contacts",
                        "/api/contacts/**",
                        "/api/organizations",
                        "/api/organizations/**",
                        "/api/projects",
                        "/api/projects/**",
                        "/api/tasks",
                        "/api/tasks/**",
                        "/api/teams",
                        "/api/teams/**",
                        "/api/create-team",
                        "/api/create-team/**",
                        "/api/team-members",
                        "/api/team-members/**",
                        "/api/roles",
                        "/api/roles/**",
                        "/api/user-permissions",
                        "/api/user-permissions/**",
                        "/api/data-scope",
                        "/api/data-scope/**",
                        "/api/attendance",
                        "/api/attendance/**",
                        "/api/trash",
                        "/api/trash/**",
                        "/api/negotiations",
                        "/api/negotiations/**",
                        "/api/documents",
                        "/api/documents/**",
                        "/api/integrations",
                        "/api/integrations/**",
                        "/api/superadmin/companies",
                        "/api/superadmin/companies/**",
                        "/api/super-admin",
                        "/api/super-admin/**",
                        "/api/view/**",
                        "/api/download/**",
                        "/api/files/**",
                        "/api-file/**",
                        "/ws-crm/**"
                    ).permitAll()
                    .anyRequest().authenticated()
                )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(customCorsFilter, org.springframework.security.web.access.channel.ChannelProcessingFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
