package com.example.starhub.dto.request;

import com.example.starhub.entity.enums.Duration;
import com.example.starhub.entity.enums.RecruitmentType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CreatePostRequestDto {

    private RecruitmentType recruitmentType;
    private Integer maxParticipants;
    private Duration duration;
    private LocalDate endDate;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String title;
    private String description;
    private String goal;
    private String otherInfo;

}