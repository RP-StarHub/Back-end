package com.example.starhub.service;

import com.example.starhub.config.Redis.RedisService;
import com.example.starhub.service.filter.LogoutFilter;
import com.example.starhub.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutTest {

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private RedisService redisService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private LogoutFilter logoutFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldPassThroughForNonLogoutRequests() throws ServletException, IOException {
        request.setRequestURI("/api/v1/other");
        request.setMethod("POST");

        logoutFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturnBadRequestIfNoRefreshToken() throws ServletException, IOException {
        request.setRequestURI("/api/v1/logout");
        request.setMethod("POST");

        logoutFilter.doFilter(request, response, filterChain);

        assertEquals(400, response.getStatus());
    }

    @Test
    void shouldReturnUnauthorizedIfTokenIsExpired() throws ServletException, IOException {
        request.setRequestURI("/api/v1/logout");
        request.setMethod("POST");
        request.setCookies(new javax.servlet.http.Cookie("refresh", "expiredToken"));

        when(jwtUtil.isExpired("expiredToken")).thenReturn(true);

        logoutFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldReturnUnauthorizedIfTokenCategoryIsInvalid() throws ServletException, IOException {
        request.setRequestURI("/api/v1/logout");
        request.setMethod("POST");
        request.setCookies(new javax.servlet.http.Cookie("refresh", "invalidCategoryToken"));

        when(jwtUtil.isExpired("invalidCategoryToken")).thenReturn(false);
        when(jwtUtil.getCategory("invalidCategoryToken")).thenReturn("access");

        logoutFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldReturnUnauthorizedIfTokenNotInRedis() throws ServletException, IOException {
        request.setRequestURI("/api/v1/logout");
        request.setMethod("POST");
        request.setCookies(new javax.servlet.http.Cookie("refresh", "validToken"));

        when(jwtUtil.isExpired("validToken")).thenReturn(false);
        when(jwtUtil.getCategory("validToken")).thenReturn("refresh");
        when(jwtUtil.getUsername("validToken")).thenReturn("testUser");
        when(redisService.getValues("refresh_token:testUser")).thenReturn(Optional.empty());

        logoutFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldLogoutSuccessfullyIfValidTokenProvided() throws ServletException, IOException {
        request.setRequestURI("/api/v1/logout");
        request.setMethod("POST");
        request.setCookies(new javax.servlet.http.Cookie("refresh", "validToken"));

        when(jwtUtil.isExpired("validToken")).thenReturn(false);
        when(jwtUtil.getCategory("validToken")).thenReturn("refresh");
        when(jwtUtil.getUsername("validToken")).thenReturn("testUser");
        when(redisService.getValues("refresh_token:testUser")).thenReturn(Optional.of("validToken"));

        logoutFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(redisService).deleteValues("refresh_token:testUser");
    }
}
