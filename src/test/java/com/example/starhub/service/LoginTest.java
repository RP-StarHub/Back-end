package com.example.starhub.service;

import com.example.starhub.config.Redis.RedisService;
import com.example.starhub.dto.request.CreateUserRequestDto;
import com.example.starhub.dto.security.CustomUserDetails;
import com.example.starhub.service.filter.LoginFilter;
import com.example.starhub.util.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(MockitoExtension.class)
class LoginTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private LoginFilter loginFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper();
    }

    @Test
    void attemptAuthentication_Success() throws IOException {

        CreateUserRequestDto dto = new CreateUserRequestDto("testUser", "password123");
        request.setContent(objectMapper.writeValueAsString(dto).getBytes(StandardCharsets.UTF_8));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Authentication authentication = new UsernamePasswordAuthenticationToken("testUser", "password123");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        Authentication result = loginFilter.attemptAuthentication(request, response);

        assertNotNull(result);
        assertEquals("testUser", result.getPrincipal());
    }

}
