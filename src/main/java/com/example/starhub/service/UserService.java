package com.example.starhub.service;

import com.example.starhub.dto.request.CreateProfileRequestDto;
import com.example.starhub.dto.request.CreateUserRequestDto;
import com.example.starhub.dto.request.UsernameCheckRequestDto;
import com.example.starhub.dto.response.ProfileResponseDto;
import com.example.starhub.dto.response.UserResponseDto;
import com.example.starhub.dto.response.UsernameCheckResponseDto;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.exception.*;
import com.example.starhub.repository.UserRepository;
import com.example.starhub.response.code.ErrorCode;
import com.example.starhub.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long ACCESS_TOKEN_EXPIRATION = 600000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JWTUtil jwtUtil;
    private final RedisService redisService;

    /**
     * 1차 회원가입
     * - 사용자명과 비밀번호를 DB에 저장합니다.
     *
     * @param createUserRequestDto 1차 회원가입 요청 DTO
     * @return UserResponseDto 1차 회원가입 응답 DTO
     */
    public UserResponseDto registerUser(CreateUserRequestDto createUserRequestDto) {

        // 아아디 관련 작업
        String username = createUserRequestDto.getUsername();
        if(userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 비밀번호 암호화
        String password = bCryptPasswordEncoder.encode(createUserRequestDto.getPassword());

        // DB 저장
        UserEntity user = UserEntity.createUser(username, password);
        userRepository.save(user);

        return UserResponseDto.builder()
                .username(user.getUsername())
                .isProfileComplete(user.getIsProfileComplete())
                .build();
    }

    /**
     * 사용자명 중복 확인
     *
     * @param usernameCheckRequestDto 사용자명 중복 요청 DTO
     * @return UsernameCheckResponseDto 사용자명 중복 여부가 담긴 DTO
     */
    @Transactional(readOnly = true)
    public UsernameCheckResponseDto checkUsernameDuplicate(UsernameCheckRequestDto usernameCheckRequestDto) {
        String username = usernameCheckRequestDto.getUsername();
        boolean isAvailable = !userRepository.existsByUsername(username);
        return new UsernameCheckResponseDto(username, isAvailable);
    }

    /**
     * 프로필 생성하기(2차 회원가입)
     *
     * @param userId 유저 아이디
     * @param createProfileRequestDto 프로필 생성 요청 DTO
     * @return ProfileResponseDto 프로필 생성 응답 DTO
     */
    public ProfileResponseDto createUserProfile(Long userId, CreateProfileRequestDto createProfileRequestDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(
                createProfileRequestDto.getProfileImage(),
                createProfileRequestDto.getNickname(),
                createProfileRequestDto.getName(),
                createProfileRequestDto.getAge(),
                createProfileRequestDto.getBio(),
                createProfileRequestDto.getEmail(),
                createProfileRequestDto.getPhoneNumber()
        );

        return ProfileResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional(readOnly = true)
    public Long findUserIdByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }

    /**
     * 토큰 재발급
     *
     * @param refreshToken 재발급을 요청한 기존의 Refresh 토큰
     * @return 새로운 Access 토큰과 Refresh 토큰을 콤마로 구분하여 반환
     */
    public String reissueToken(String refreshToken) {
        validateRefreshToken(refreshToken);

        // 사용자 정보 가져오기
        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        // Redis에서 토큰의 블랙리스트 상태 확인
        String refreshTokenKey = validateRedisToken(refreshToken, username);

        // 새로운 Access 및 Refresh 토큰 생성
        String newAccessToken = jwtUtil.createJwt("access", username, role, ACCESS_TOKEN_EXPIRATION);
        String newRefreshToken = jwtUtil.createJwt("refresh", username, role, REFRESH_TOKEN_EXPIRATION);

        // 새 Refresh 토큰 Redis에 저장
        redisService.deleteValues(refreshTokenKey);
        redisService.setValues(refreshTokenKey, newRefreshToken, Duration.ofMillis(REFRESH_TOKEN_EXPIRATION));

        return newAccessToken + "," + newRefreshToken;
    }

    private String validateRedisToken(String refreshToken, String username) {
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;
        Optional<String> storedTokenOptional = redisService.getValues(refreshTokenKey);

        // 값이 없는 경우 예외 처리
        String storedToken = storedTokenOptional.orElseThrow(() ->
                new TokenNotFoundInRedisException(ErrorCode.TOKEN_NOT_FOUND)
        );

        // 값이 다른 경우 예외 처리
        if (!MessageDigest.isEqual(refreshToken.getBytes(), storedToken.getBytes())) {
            throw new InvalidTokenException(ErrorCode.INVALID_TOKEN);
        }
        return refreshTokenKey;
    }

    private void validateRefreshToken(String refreshToken) {
        if (jwtUtil.isExpired(refreshToken)) {
            throw new TokenExpiredException(ErrorCode.TOKEN_EXPIRED);
        }

        String category = jwtUtil.getCategory(refreshToken);
        if (!"refresh".equals(category)) {
            throw new InvalidTokenCategoryException(ErrorCode.INVALID_TOKEN_CATEGORY);
        }
    }

}
