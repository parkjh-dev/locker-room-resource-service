package com.lockerroom.resourceservice.sport.service;

import com.lockerroom.resourceservice.sport.dto.response.ContinentResponse;
import com.lockerroom.resourceservice.sport.dto.response.CountryResponse;
import com.lockerroom.resourceservice.sport.dto.response.LeagueResponse;
import com.lockerroom.resourceservice.sport.dto.response.SportResponse;

import java.util.List;

public interface SportService {

    List<SportResponse> getSports();

    /** 대륙 전체 목록. */
    List<ContinentResponse> getContinents();

    /** 해당 종목의 리그가 등록된 국가 목록만 반환 (한국 우선 정렬). */
    List<CountryResponse> getCountriesBySport(Long sportId);

    /** 종목·국가 조합으로 운영되는 리그 목록. 알 수 없는 종목은 빈 배열. */
    List<LeagueResponse> getLeaguesByCountryAndSport(Long sportId, Long countryId);
}
