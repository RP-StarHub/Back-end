package com.example.starhub.repository;

import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.enums.Duration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MeetingRepositoryCustom {

    Page<MeetingEntity> searchMeetings(String title, Integer minParticipants, Integer maxParticipants,
                                       List<Long> techStackIds, String location, Duration duration,
                                       Double minLatitude, Double maxLatitude, Double minLongitude, Double maxLongitude,
                                       Pageable pageable);
}
