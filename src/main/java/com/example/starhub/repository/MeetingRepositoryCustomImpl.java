package com.example.starhub.repository;

import com.example.starhub.entity.MeetingEntity;
import com.example.starhub.entity.QMeetingEntity;
import com.example.starhub.entity.QMeetingTechStackEntity;
import com.example.starhub.entity.QTechStackEntity;
import com.example.starhub.entity.enums.Duration;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MeetingRepositoryCustomImpl implements MeetingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MeetingEntity> searchMeetings(String title, Integer minParticipants, Integer maxParticipants,
                                              List<String> techStacks, String location, Duration duration,
                                              Pageable pageable) {

        QMeetingEntity meeting = QMeetingEntity.meetingEntity;
        QMeetingTechStackEntity meetingTechStack = QMeetingTechStackEntity.meetingTechStackEntity;
        QTechStackEntity techStack = QTechStackEntity.techStackEntity;
        BooleanBuilder builder = new BooleanBuilder();

        if (title != null && !title.isEmpty()) {
            builder.and(meeting.title.containsIgnoreCase(title));
        }
        if (minParticipants != null && maxParticipants != null) {
            builder.and(meeting.maxParticipants.between(minParticipants, maxParticipants));
        }
        if (techStacks != null && !techStacks.isEmpty()) {
            builder.and(meeting.id.in(
                    queryFactory.select(meetingTechStack.meeting.id)
                            .from(meetingTechStack)
                            .join(meetingTechStack.techStack, techStack)
                            .where(techStack.name.in(techStacks))
            ));
        }
        if (location != null && !location.isEmpty()) {
            builder.and(meeting.location.eq(location));
        }
        if (duration != null) {
            builder.and(meeting.duration.eq(duration));
        }

        List<MeetingEntity> results = queryFactory.selectFrom(meeting)
                .where(builder)
                .orderBy(meeting.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory.selectFrom(meeting)
                .where(builder)
                .fetchCount();

        return new PageImpl<>(results, pageable, total);
    }
}
