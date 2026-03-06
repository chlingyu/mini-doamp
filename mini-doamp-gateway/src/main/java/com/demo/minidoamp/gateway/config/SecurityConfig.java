package com.demo.minidoamp.gateway.config;

import com.demo.minidoamp.gateway.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .antMatchers(HttpMethod.GET, "/api/auth/userInfo").authenticated()
                .antMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .antMatchers("/api/users", "/api/users/**", "/api/roles", "/api/roles/**", "/api/depts", "/api/depts/**").hasAuthority("system.user")
                .antMatchers("/api/warn/indexes", "/api/warn/indexes/**").hasAuthority("warn.index")
                .antMatchers("/api/warn/rules", "/api/warn/rules/**").hasAuthority("warn.rule")
                .antMatchers("/api/warn/records", "/api/warn/records/**").hasAuthority("warn.record")
                .antMatchers("/api/msg/records", "/api/msg/records/**").hasAuthority("warn.message")
                .antMatchers("/api/sop/workflows", "/api/sop/workflows/**").hasAuthority("sop.workflow")
                .antMatchers("/api/sop/task-templates", "/api/sop/task-templates/**").hasAuthority("sop.template")
                .antMatchers("/api/sop/tasks", "/api/sop/tasks/**", "/api/sop/task-execs", "/api/sop/task-execs/**").hasAnyAuthority("sop.task", "sop.approve")
                .antMatchers("/api/dict", "/api/dict/**", "/api/cache/refresh/dict", "/api/cache/refresh/dict/**").hasAuthority("system.dict")
                .antMatchers("/api/system/job/**", "/api/cache/refresh/index", "/api/cache/refresh/all", "/api/db/**").hasAuthority("system.job")
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            .and()
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
