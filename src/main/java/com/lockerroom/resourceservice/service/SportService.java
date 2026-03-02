package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.response.SportResponse;

import java.util.List;

public interface SportService {

    List<SportResponse> getSports();
}
