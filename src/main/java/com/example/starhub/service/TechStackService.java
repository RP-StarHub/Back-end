package com.example.starhub.service;

import com.example.starhub.dto.response.TechStackResponseDto;
import com.example.starhub.entity.TechStackEntity;
import com.example.starhub.entity.enums.TechCategory;
import com.example.starhub.repository.TechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechStackService {

    private final TechStackRepository techStackRepository;

    /**
     * 기술 스택 불러오기
     * - 누구나 불러올 수 있음
     * - 기술 스택 카테고리가 OTHER가 아닌 기술 스택 목록 불러오기
     *
     * @return 기술 스택 정보가 담긴 DTO
     */
    public List<TechStackResponseDto> getTechStack() {
        List<TechStackEntity> techStackEntities = techStackRepository.findByCategoryNot(TechCategory.OTHER);

        return techStackEntities
                .stream()
                .map(TechStackResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}
