package com.example.starhub.integration;

import com.example.starhub.entity.UserEntity;
import com.example.starhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        UserEntity userEntity = UserEntity.createUser("testUser", new BCryptPasswordEncoder().encode("testPassword"));
        userRepository.save(userEntity);
    }

    @Test
    public void testSuccessfulAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/login")
                        .contentType("application/json")
                        .content("{\"username\": \"testUser\", \"password\": \"testPassword\"}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    public void testFailedAuthentication_withWrongUsername() throws Exception {
        mockMvc.perform(post("/api/v1/login")
                        .contentType("application/json")
                        .content("{\"username\": \"wrongUsername\", \"password\": \"testPassword\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testFailedAuthentication_withWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/login")
                        .contentType("application/json")
                        .content("{\"username\": \"testUser\", \"password\": \"wrongPassword\"}"))
                .andExpect(status().isUnauthorized());
    }
}