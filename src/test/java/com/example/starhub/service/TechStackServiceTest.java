package com.example.starhub.service;

import com.example.starhub.dto.response.TechStackResponseDto;
import com.example.starhub.entity.TechStackEntity;
import com.example.starhub.entity.enums.TechCategory;
import com.example.starhub.repository.TechStackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechStackServiceTest {

    @Mock
    private TechStackRepository techStackRepository;

    @InjectMocks
    private TechStackService techStackService;

    private List<TechStackEntity> techStacks;

    @BeforeEach
    void setUp() {
        techStacks = Arrays.asList(
                new TechStackEntity(1L, "Java", TechCategory.BACKEND),
                new TechStackEntity(2L, "React", TechCategory.FRONTEND)
        );
    }

    @Test
    void getTechStack_Success() {
        when(techStackRepository.findByCategoryNot(TechCategory.OTHER)).thenReturn(techStacks);

        List<TechStackResponseDto> result = techStackService.getTechStack();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        assertThat(result.get(1).getName()).isEqualTo("React");

        verify(techStackRepository, times(1)).findByCategoryNot(TechCategory.OTHER);
    }

    @Test
    void getTechStack_EmptyList() {
        when(techStackRepository.findByCategoryNot(TechCategory.OTHER)).thenReturn(Collections.emptyList());

        List<TechStackResponseDto> result = techStackService.getTechStack();

        assertThat(result).isEmpty();

        verify(techStackRepository, times(1)).findByCategoryNot(TechCategory.OTHER);
    }

    @Test
    void getTechStack_RepositoryThrowsException() {
        when(techStackRepository.findByCategoryNot(TechCategory.OTHER)).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> techStackService.getTechStack());

        verify(techStackRepository, times(1)).findByCategoryNot(TechCategory.OTHER);
    }
}