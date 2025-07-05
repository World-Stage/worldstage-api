package com.jonathanfletcher.worldstage_api.spring.security.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RtmpSecretFilter extends OncePerRequestFilter {
    @Value("${spring.security.client.nginx.secret}")
    private static String SECRET;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (("/stream/publish".equals(path) || "/stream/unpublish".equals(path)) &&
                SECRET.equals(request.getParameter("secret"))) {
            // Allow the request through without authentication
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
