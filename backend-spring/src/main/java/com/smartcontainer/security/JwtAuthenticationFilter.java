package com.smartcontainer.security;

import com.smartcontainer.entity.User;
import com.smartcontainer.entity.Session;
import com.smartcontainer.repository.UserRepository;
import com.smartcontainer.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT authentication filter — extracts Bearer token, validates it, and sets
 * the Spring Security context. Mirrors the Node.js requireAuth middleware.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            Long sessionId = jwtTokenProvider.getSessionIdFromToken(token);

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getIsActive())) {
                filterChain.doFilter(request, response);
                return;
            }

            // Validate session if present
            if (sessionId != null) {
                Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
                if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                // Update last_seen
                Session session = sessionOpt.get();
                session.setLastSeen(LocalDateTime.now());
                sessionRepository.save(session);
            }

            User user = userOpt.get();
            // Store user in request attributes for controllers to access
            request.setAttribute("currentUser", user);
            request.setAttribute("tokenSessionId", sessionId);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception e) {
            // Token parsing failed — continue without authentication
        }

        filterChain.doFilter(request, response);
    }
}
