package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.response.SportResponse;
import com.lockerroom.resourceservice.mapper.PostMapper;
import com.lockerroom.resourceservice.repository.SportRepository;
import com.lockerroom.resourceservice.service.SportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SportServiceImpl implements SportService {

    private final SportRepository sportRepository;
    private final PostMapper postMapper;

    @Override
    public List<SportResponse> getSports() {
        return sportRepository.findByIsActiveTrue().stream()
                .map(postMapper::toSportResponse)
                .toList();
    }
}
