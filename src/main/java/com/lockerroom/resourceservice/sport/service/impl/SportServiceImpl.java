package com.lockerroom.resourceservice.sport.service.impl;

import com.lockerroom.resourceservice.sport.dto.response.SportResponse;
import com.lockerroom.resourceservice.sport.mapper.SportMapper;
import com.lockerroom.resourceservice.sport.repository.SportRepository;
import com.lockerroom.resourceservice.sport.service.SportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SportServiceImpl implements SportService {

    private final SportRepository sportRepository;
    private final SportMapper sportMapper;

    @Override
    public List<SportResponse> getSports() {
        return sportRepository.findByIsActiveTrue().stream()
                .map(sportMapper::toSportResponse)
                .toList();
    }
}
