package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.response.SportResponse;
import com.lockerroom.resourceservice.dto.response.TeamResponse;

import java.util.List;

public interface SportService {

    List<SportResponse> getSports();

    List<TeamResponse> getTeamsBySport(Long sportId);
}
