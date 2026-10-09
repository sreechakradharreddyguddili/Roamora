package com.weatherwise.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String REQUEST_ID_HEADER =
            "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime =
                System.currentTimeMillis();

        String requestId =
                UUID.randomUUID().toString();

        String method =
                request.getMethod();

        String uri =
                request.getRequestURI();

        response.setHeader(
                REQUEST_ID_HEADER,
                requestId
        );

        logger.info(
                "Request started | requestId={} method={} uri={}",
                requestId,
                method,
                uri
        );

        try {

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            long executionTime =
                    System.currentTimeMillis()
                            - startTime;

            logger.info(
                    "Request completed | requestId={} method={} uri={} status={} duration={}ms",
                    requestId,
                    method,
                    uri,
                    response.getStatus(),
                    executionTime
            );
        }
    }
}