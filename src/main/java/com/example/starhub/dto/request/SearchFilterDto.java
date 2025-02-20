package com.example.starhub.dto.request;

import com.example.starhub.entity.enums.Duration;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SearchFilterDto {

    private Integer minParticipants;
    private Integer maxParticipants;
    private List<String> techStacks;
    private String location;
    private Duration duration;

    private Double minLatitude;
    private Double maxLatitude;
    private Double minLongitude;
    private Double maxLongitude;
}
