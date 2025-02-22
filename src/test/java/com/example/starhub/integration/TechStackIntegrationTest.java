package com.example.starhub.integration;

import com.example.starhub.dto.request.TechStackDto;
import com.example.starhub.dto.response.TechStackResponseDto;
import com.example.starhub.entity.TechStackEntity;
import com.example.starhub.entity.enums.TechCategory;
import com.example.starhub.repository.TechStackRepository;
import com.example.starhub.service.TechStackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TechStackIntegrationTest {

    @Autowired
    private TechStackService techStackService;

    @Autowired
    private TechStackRepository techStackRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 데이터 초기화
        techStackRepository.deleteAll();

        // 기본 데이터 추가
        saveTechStack("React", TechCategory.FRONTEND);
        saveTechStack("Spring", TechCategory.BACKEND);
        saveTechStack("React Native", TechCategory.MOBILE);
        saveTechStack("OtherTool", TechCategory.OTHER);

    }

    @Test
    void getTechStack_shouldReturnTechStackList_whenNotEmpty() {
        // When
        List<TechStackResponseDto> techStacks = techStackService.getTechStack();

        // Then
        assertEquals(3, techStacks.size());
        assertTrue(techStacks.stream().allMatch(stack -> stack.getCategory() != TechCategory.OTHER));
    }

    @Test
    void getTechStack_shouldReturnEmptyList_whenNoDataExists() {
        // Given
        techStackRepository.deleteAll();

        // When
        List<TechStackResponseDto> techStacks = techStackService.getTechStack();

        // Then
        assertTrue(techStacks.isEmpty());
    }

    private TechStackEntity saveTechStack(String name, TechCategory category) {
        return techStackRepository.save(TechStackEntity.createTechStackEntity(buildTechStackDto(name, category)));
    }

    private TechStackDto buildTechStackDto(String name, TechCategory category) {
        return TechStackDto.builder()
                .name(name)
                .category(category)
                .build();
    }

}