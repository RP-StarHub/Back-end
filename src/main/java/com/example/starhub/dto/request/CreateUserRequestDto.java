package com.example.starhub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CreateUserRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Pattern(
            regexp = "^[a-zA-Z0-9-_]{6,12}$",
            message = "아이디는 6~12자 영문, 숫자, 기호(-, _)만 사용 가능합니다."
    )
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*[0-9]|.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,20}$",
            message = "비밀번호는 8~20자 이내로 숫자, 특수문자, 영문자 중 2가지 이상 조합이어야 합니다."
    )
    private String password;
}
