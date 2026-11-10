package com.opticsappbg.opticsapp.security;

import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ActiveUserSessionFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public ActiveUserSessionFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof AuthenticationDetails details) {

            User user = userRepository.findById(details.getUserId())
                    .orElse(null);

            if (user == null || !user.isActive()) {

                SecurityContextHolder.clearContext();

                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }

                response.sendRedirect("/login?disabled");
                return;
            }

            if (user.getRole() != details.getRole()
                    || !user.getUsername().equals(details.getUsername())) {

                SecurityContextHolder.clearContext();

                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }

                response.sendRedirect("/login?profileChanged");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

