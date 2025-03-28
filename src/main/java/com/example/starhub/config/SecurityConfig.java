package com.example.starhub.config;

import com.example.starhub.config.redis.RedisService;
import com.example.starhub.service.filter.JWTFilter;
import com.example.starhub.service.filter.LoginFilter;
import com.example.starhub.service.filter.LogoutFilter;
import com.example.starhub.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JWTUtil jwtUtil;
    private final RedisService redisService;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // cors 설정
        http
                .cors((corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {

                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                        CorsConfiguration configuration = new CorsConfiguration();

                        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
                        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
                        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedHeaders(Collections.singletonList("*"));
                        configuration.setMaxAge(3600L);

                        configuration.setExposedHeaders(Collections.singletonList("Authorization"));

                        return configuration;
                    }
                })));

        // csrf disable
        http
                .csrf((auth) -> auth.disable());

        // From 로그인 방식 disable
        http
                .formLogin((auth) -> auth.disable());

        // http basic 인증 방식 disable
        http
                .httpBasic((auth) -> auth.disable());

        // 경로별 인가 작업
        http
                .authorizeHttpRequests((auth) -> auth
                        // 공개 접근 경로
                        .antMatchers("/api/v1/register", "/api/v1/users/check", "/api/v1/login", "/api/v1/reissue").permitAll()
                        .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/starhub-api/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/v1/meetings/{meetingId}").permitAll()
                        .antMatchers(HttpMethod.POST, "/api/v1/meetings/search").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/v1/techStacks").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/v1/meetings/popular/projects", "/api/v1/meetings/popular/studies", "/api/v1/meetings/popular/expiring").permitAll()
                        .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                        // 관리자만 접근 가능 경로
                        .antMatchers(HttpMethod.POST, "/api/v1/techStacks").hasRole("ADMIN")

                        // 모든 인증된 사용자
                        .anyRequest().authenticated()
                );


        // 로그아웃 필터 추가
        http
                .addFilterBefore(new LogoutFilter(jwtUtil, redisService), org.springframework.security.web.authentication.logout.LogoutFilter.class);

        // JWTFilter 등록
        http
                .addFilterBefore(new JWTFilter(jwtUtil), LoginFilter.class);

        // 로그인 필터 추가
        http
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, redisService), UsernamePasswordAuthenticationFilter.class);

        // 세션 설정
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
