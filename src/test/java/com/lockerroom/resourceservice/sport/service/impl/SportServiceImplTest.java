package com.lockerroom.resourceservice.sport.service.impl;

import com.lockerroom.resourceservice.sport.dto.response.SportResponse;
import com.lockerroom.resourceservice.sport.mapper.SportMapper;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import com.lockerroom.resourceservice.sport.repository.SportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportServiceImplTest {

    @Mock private SportRepository sportRepository;
    @Mock private SportMapper sportMapper;

    @InjectMocks private SportServiceImpl sportService;

    private Sport soccer;
    private Sport baseball;

    @BeforeEach
    void setUp() {
        soccer = Sport.builder()
                .id(1L)
                .nameKo("축구")
                .nameEn("Football")
                .isActive(true)
                .build();

        baseball = Sport.builder()
                .id(2L)
                .nameKo("야구")
                .nameEn("Baseball")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getSports")
    class GetSports {

        @Test
        @DisplayName("should return all active sports")
        void getSports_success() {
            SportResponse soccerResponse = new SportResponse(1L, "축구", "Football", true);
            SportResponse baseballResponse = new SportResponse(2L, "야구", "Baseball", true);

            when(sportRepository.findByIsActiveTrue()).thenReturn(List.of(soccer, baseball));
            when(sportMapper.toSportResponse(soccer)).thenReturn(soccerResponse);
            when(sportMapper.toSportResponse(baseball)).thenReturn(baseballResponse);

            List<SportResponse> result = sportService.getSports();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).nameKo()).isEqualTo("축구");
            assertThat(result.get(1).nameKo()).isEqualTo("야구");
        }

        @Test
        @DisplayName("should return empty list when no active sports")
        void getSports_empty() {
            when(sportRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

            List<SportResponse> result = sportService.getSports();

            assertThat(result).isEmpty();
        }
    }
}
