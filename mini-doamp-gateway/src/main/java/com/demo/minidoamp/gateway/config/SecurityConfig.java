package com.demo.minidoamp.gateway.config;

import com.demo.minidoamp.gateway.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                // P0-b: Actuator probes + Prometheus scrape（生产阶段建议改为 IP 白名单或独立 mgmt port）
                .requestMatchers("/actuator/health", "/actuator/health/**",
                                 "/actuator/info", "/actuator/prometheus").permitAll()
                // 其他 /actuator/** 默认 denyAll，防止未来暴露 env/beans/mappings/configprops/loggers
                // 时被 anyRequest().permitAll() 兜底意外开放（见 P0 /security-review finding）
                .requestMatchers("/actuator/**").denyAll()
                // P0-d: SpringDoc OpenAPI + Swagger UI
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**",
                                 "/swagger-ui", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/userInfo").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .requestMatchers("/api/users", "/api/users/**", "/api/roles", "/api/roles/**", "/api/depts", "/api/depts/**").hasAuthority("system.user")
                .requestMatchers("/api/warn/indexes", "/api/warn/indexes/**").hasAuthority("warn.index")
                .requestMatchers("/api/warn/rules", "/api/warn/rules/**").hasAuthority("warn.rule")
                .requestMatchers("/api/warn/records", "/api/warn/records/**").hasAuthority("warn.record")
                .requestMatchers("/api/msg/records", "/api/msg/records/**").hasAuthority("warn.message")
                .requestMatchers("/api/sop/workflows", "/api/sop/workflows/**").hasAuthority("sop.workflow")
                .requestMatchers("/api/sop/task-templates", "/api/sop/task-templates/**").hasAuthority("sop.template")
                .requestMatchers("/api/sop/tasks", "/api/sop/tasks/**", "/api/sop/task-execs", "/api/sop/task-execs/**").hasAnyAuthority("sop.task", "sop.approve")
                .requestMatchers("/api/dict", "/api/dict/**", "/api/cache/refresh/dict", "/api/cache/refresh/dict/**").hasAuthority("system.dict")
                .requestMatchers("/api/system/job/**", "/api/cache/refresh/index", "/api/cache/refresh/all", "/api/db/**").hasAuthority("system.job")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
