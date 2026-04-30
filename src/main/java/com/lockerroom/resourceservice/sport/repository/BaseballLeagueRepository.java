package com.lockerroom.resourceservice.sport.repository;

import com.lockerroom.resourceservice.sport.model.entity.BaseballLeague;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BaseballLeagueRepository extends JpaRepository<BaseballLeague, Long> {
    List<BaseballLeague> findByCountryId(Long countryId);

    /** sport·country 조합으로 운영되는 리그 목록 (cascading API). */
    List<BaseballLeague> findBySportIdAndCountryId(Long sportId, Long countryId);

    /** 해당 sport의 리그가 운영되는 국가 ID 목록 (distinct). */
    @Query("SELECT DISTINCT l.country.id FROM BaseballLeague l WHERE l.sport.id = :sportId")
    List<Long> findDistinctCountryIdsBySport(@Param("sportId") Long sportId);
}
