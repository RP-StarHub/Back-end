package com.example.starhub.service;

import com.example.starhub.config.redis.RedisService;
import com.example.starhub.dto.request.CreateProfileRequestDto;
import com.example.starhub.dto.request.CreateUserRequestDto;
import com.example.starhub.dto.request.UsernameCheckRequestDto;
import com.example.starhub.dto.response.ProfileSummaryResponseDto;
import com.example.starhub.dto.response.UserResponseDto;
import com.example.starhub.dto.response.UsernameCheckResponseDto;
import com.example.starhub.entity.UserEntity;
import com.example.starhub.exception.*;
import com.example.starhub.repository.UserRepository;
import com.example.starhub.util.JWTUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private RedisService redisService;

    @Test
    void testRegisterUser_Success() {
        CreateUserRequestDto request = new CreateUserRequestDto("testUser", "testPassword");

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(bCryptPasswordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(UserEntity.createUser("testUser", "testPassword"));

        UserResponseDto response = userService.registerUser(request);

        assertEquals("testUser", response.getUsername());
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    void should_ThrowException_When_UsernameAlreadyExists() {
        CreateUserRequestDto requestDto = new CreateUserRequestDto("testUser", "testPassword");
        when(userRepository.existsByUsername("testUser")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.registerUser(requestDto));
    }

    @Test
    void should_EncryptPassword_When_RegisterUser() {
        String rawPassword = "testPassword";
        CreateUserRequestDto requestDto = new CreateUserRequestDto("testUser", rawPassword);
        UserEntity userEntity = UserEntity.createUser("testUser", "testPassword");

        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(userRepository.existsByUsername("testUser")).thenReturn(false);
        when(bCryptPasswordEncoder.encode(rawPassword)).thenReturn("encodedPassword");

        userService.registerUser(requestDto);

        verify(bCryptPasswordEncoder, times(1)).encode(rawPassword);
    }

    @Test
    void should_ThrowException_With_ErrorMessage_When_UsernameAlreadyExists() {
        CreateUserRequestDto requestDto = new CreateUserRequestDto("testUser", "testPassword");
        when(userRepository.existsByUsername("testUser")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.registerUser(requestDto));
    }

    @Test
    void should_ThrowException_When_DbSaveFails() {
        CreateUserRequestDto requestDto = new CreateUserRequestDto("testUser", "testPassword");
        when(userRepository.existsByUsername("testUser")).thenReturn(false);
        when(bCryptPasswordEncoder.encode("testPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenThrow(new RuntimeException("DB save failed"));

        assertThrows(RuntimeException.class, () -> userService.registerUser(requestDto));
    }

    @Test
    void should_ReturnTrue_When_UsernameIsAvailable() {
        String username = "newuser";
        UsernameCheckRequestDto requestDto = new UsernameCheckRequestDto(username);
        when(userRepository.existsByUsername(username)).thenReturn(false);

        UsernameCheckResponseDto response = userService.checkUsernameDuplicate(requestDto);

        assertTrue(response.isAvailable());
        assertEquals(username, response.getUsername());
    }

    @Test
    void should_ReturnFalse_When_UsernameIsAlreadyTaken() {
        String username = "existinguser";
        UsernameCheckRequestDto requestDto = new UsernameCheckRequestDto(username);
        when(userRepository.existsByUsername(username)).thenReturn(true);

        UsernameCheckResponseDto response = userService.checkUsernameDuplicate(requestDto);

        assertFalse(response.isAvailable());
        assertEquals(username, response.getUsername());
    }

    @Test
    void should_ThrowException_When_UserNotFound() {
        String username = "nonexistentuser";
        CreateProfileRequestDto requestDto = buildCreateProfileRequestDto();
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.createUserProfile(username, requestDto));
    }

    @Test
    void should_ThrowException_When_ProfileAlreadyExists() {
        UserEntity userEntity = mock(UserEntity.class);

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(userEntity));
        when(userEntity.getIsProfileComplete()).thenReturn(true);
        CreateProfileRequestDto requestDto = buildCreateProfileRequestDto();

        assertThrows(UserProfileAlreadyExistsException.class, () ->
                userService.createUserProfile("testUser", requestDto)
        );
    }

    @Test
    void should_CreateProfileSuccessfully_When_ValidUser() {
        String username = "testUser";
        CreateProfileRequestDto requestDto = buildCreateProfileRequestDto();

        UserEntity userEntity = mock(UserEntity.class);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userEntity));

        when(userEntity.getIsProfileComplete()).thenReturn(false);
        doNothing().when(userEntity).createProfile(
                anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyString()
        );

        when(userEntity.getNickname()).thenReturn("nickname");
        when(userEntity.getProfileImage()).thenReturn("image.jpg");

        ProfileSummaryResponseDto response = userService.createUserProfile(username, requestDto);

        assertNotNull(response);
        assertEquals("nickname", response.getNickname());
        assertEquals("image.jpg", response.getProfileImage());
    }

    @Test
    void shouldReissueTokens_When_ValidRefreshTokenIsProvided() {
        String refreshToken = "validRefreshToken";
        String username = "validUser";
        String role = "USER";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        when(jwtUtil.getUsername(refreshToken)).thenReturn(username);
        when(jwtUtil.getRole(refreshToken)).thenReturn(role);
        when(jwtUtil.createJwt(eq("access"), eq(username), eq(role), anyLong())).thenReturn(newAccessToken);
        when(jwtUtil.createJwt(eq("refresh"), eq(username), eq(role), anyLong())).thenReturn(newRefreshToken);

        when(jwtUtil.getCategory(refreshToken)).thenReturn("refresh");

        when(redisService.getValues(anyString())).thenReturn(Optional.of("validRefreshToken"));
        doNothing().when(redisService).deleteValues(anyString());
        doNothing().when(redisService).setValues(anyString(), eq(newRefreshToken), any(Duration.class));

        String result = userService.reissueToken(refreshToken);

        assertNotNull(result);
        String[] tokens = result.split(",");
        assertEquals(newAccessToken, tokens[0]);
        assertEquals(newRefreshToken, tokens[1]);
    }

    @Test
    void shouldThrowException_When_RefreshTokenIsExpired() {
        String expiredRefreshToken = "expiredRefreshToken";

        when(jwtUtil.isExpired(expiredRefreshToken)).thenReturn(true);

        assertThrows(TokenExpiredException.class, () -> userService.reissueToken(expiredRefreshToken));
    }
    @Test
    void shouldThrowException_When_TokenCategoryIsInvalid() {
        String invalidCategoryToken = "invalidCategoryToken";

        when(jwtUtil.getCategory(invalidCategoryToken)).thenReturn("access");

        assertThrows(InvalidTokenCategoryException.class, () -> userService.reissueToken(invalidCategoryToken));
    }

    @Test
    void shouldThrowException_When_RefreshTokenNotInRedis() {
        String refreshToken = "validRefreshToken";
        String username = "validUser";

        when(jwtUtil.getUsername(refreshToken)).thenReturn(username);
        when(jwtUtil.getCategory(refreshToken)).thenReturn("refresh");
        when(redisService.getValues(anyString())).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundInRedisException.class, () -> userService.reissueToken(refreshToken));
    }

    @Test
    void shouldThrowException_When_RefreshTokenDoesNotMatchWithRedis() {
        String refreshToken = "validRefreshToken";
        String username = "validUser";
        String redisToken = "invalidRefreshToken";

        when(jwtUtil.getUsername(refreshToken)).thenReturn(username);
        when(jwtUtil.getCategory(refreshToken)).thenReturn("refresh");
        when(redisService.getValues(anyString())).thenReturn(Optional.of(redisToken));

        assertThrows(InvalidTokenMismatchException.class, () -> userService.reissueToken(refreshToken));
    }

    private static CreateProfileRequestDto buildCreateProfileRequestDto() {
        CreateProfileRequestDto requestDto = new CreateProfileRequestDto("image.jpg", "nickname", "name", 25, "bio", "email@example.com", "010-1234-5678");
        return requestDto;
    }

}