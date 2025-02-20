package com.example.starhub.dto.request;

import com.example.starhub.entity.enums.Duration;
import lombok.Getter;

import java.util.List;

@Getter
public class SearchFilterDto {

    private Integer minParticipants;
    private Integer maxParticipants;
    private List<String> techStacks;
    private String location;
    private Duration duration;
}
