package com.example.starhub.service.filter;

import com.example.starhub.dto.response.util.ResponseUtil;
import com.example.starhub.response.code.ErrorCode;
import com.example.starhub.response.code.ResponseCode;
import com.example.starhub.config.Redis.RedisService;
import com.example.starhub.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * 로그아웃을 처리하는 필터
 * - 사용자의 로그아웃 요청을 처리하고, 해당 사용자의 Refresh Token을 검증 및 삭제합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class LogoutFilter extends GenericFilterBean {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private final JWTUtil jwtUtil;
    private final RedisService redisService;

    /**
     * doFilter 메서드
     * - 로그아웃 요청을 필터링하고, 로그아웃을 수행합니다.
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
    }

    /**
     * 실제 로그아웃 처리를 담당하는 메서드
     * - POST 방식으로 "/logout" URI 요청이 들어오면 Refresh Token을 검증하고 로그아웃을 진행합니다.
     */
    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();

        if (!requestUri.equals("/api/v1/logout") || !requestMethod.equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키에서 refresh 토큰 가져오기
        String refresh = getRefreshTokenFromCookies(request);
        if (refresh == null) {
            ResponseUtil.writeErrorResponse(response, ErrorCode.BAD_REQUEST);
            return;
        }

        String username = jwtUtil.getUsername(refresh); // 토큰에서 사용자명 추출

        // JWT 검증
        if (validateRefreshToken(response, refresh, username)) return;
        if (validateRedisToken(response, refresh, username)) return;

        // 로그아웃 처리
        logoutUser(response, refresh, username);
        ResponseUtil.writeSuccessResponse(response, ResponseCode.SUCCESS_LOGOUT, null);
    }

    /**
     * 쿠키에서 Refresh 토큰을 추출하는 메서
     *
     * @param request 요청 서블릿 객체
     * @return 추출된 Refresh Token
     */
    private String getRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("refresh")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Redis에 저장된 Refresh Token과 비교하여 유효성 검증
     *
     * @param response 응답 객체
     * @param refresh Refresh Token
     * @param username 사용자명
     * @return 유효성 검증 결과 (true: 실패, false: 성공)
     */
    private boolean validateRedisToken(HttpServletResponse response, String refresh, String username) {
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;
        Optional<String> storedTokenOptional = redisService.getValues(refreshTokenKey);

        if (storedTokenOptional.isEmpty()) {
            log.error("Token not found in Redis for user: {}", username);
            ResponseUtil.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
            return true;
        }

        if (!MessageDigest.isEqual(refresh.getBytes(), storedTokenOptional.get().getBytes())) {
            log.error("Invalid refresh token. Mismatch detected for user: {}", username);
            ResponseUtil.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
            return true;
        }
        return false;
    }

    /**
     * Refresh Token이 만료되었거나 잘못된 경우를 검증
     *
     * @param response 응답 객체
     * @param refresh Refresh Token
     * @param username 사용자명
     * @return 유효성 검증 결과 (true: 실패, false: 성공)
     */
    private boolean validateRefreshToken(HttpServletResponse response, String refresh, String username) {
        // refresh 토큰 만료 여부 확인
        if (jwtUtil.isExpired(refresh)) {
            log.error("Token expired for user: {}", username);
            ResponseUtil.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
            return true;
        }

        // 토큰의 카테고리가 "refresh"인지 확인
        String category = jwtUtil.getCategory(refresh);
        if (!category.equals("refresh")) {
            log.error("Invalid token category for user: {}", username);
            ResponseUtil.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
            return true;
        }
        return false;
    }

    /**
     * 사용자 로그아웃 처리
     * - Redis에서 Refresh Token을 삭제하고 쿠키에서 Refresh Token을 제거합니다.
     *
     * @param response 응답 객체
     * @param refresh Refresh Token
     * @param username 사용자명
     */
    private void logoutUser(HttpServletResponse response, String refresh, String username) {
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;
        redisService.deleteValues(refreshTokenKey);

        // 쿠키에서 refresh 토큰 삭제
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
