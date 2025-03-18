package com.example.starhub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.constraints.NotBlank;

@AllArgsConstructor
@Getter
public class UsernameCheckRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String username;
}
