package com.lockerroom.resourceservice.sport.service;

import com.lockerroom.resourceservice.sport.dto.response.SportResponse;

import java.util.List;

public interface SportService {

    List<SportResponse> getSports();
}
