package com.demo.minidoamp.gateway.filter;

import com.demo.minidoamp.core.filter.MdcRequestFilter;
import com.demo.minidoamp.gateway.config.JwtUtil;
import com.demo.minidoamp.gateway.service.PermissionService;
import com.demo.minidoamp.gateway.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token)
                    && jwtUtil.isAccessToken(token)
                    && !tokenBlacklistService.isBlacklisted(token)) {
                Claims claims = jwtUtil.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                // 把 userId 写进 MDC 的时机尽量往前靠：PermissionService 内部也会查表写日志，
                // 若等 authentication 设好再写 MDC，那几行 SQL 日志里 user 字段会是空的。
                if (userId != null) {
                    MDC.put(MdcRequestFilter.MDC_USER_ID, String.valueOf(userId));
                }
                List<SimpleGrantedAuthority> authorities = permissionService.resolvePermissionsByUserId(userId)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId, null,
                                authorities
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
