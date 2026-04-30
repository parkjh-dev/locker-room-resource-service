package com.lockerroom.resourceservice.sport.mapper;

import com.lockerroom.resourceservice.sport.dto.response.ContinentResponse;
import com.lockerroom.resourceservice.sport.dto.response.CountryResponse;
import com.lockerroom.resourceservice.sport.dto.response.LeagueResponse;
import com.lockerroom.resourceservice.sport.dto.response.SportResponse;
import com.lockerroom.resourceservice.sport.dto.response.TeamResponse;
import com.lockerroom.resourceservice.sport.model.entity.BaseballLeague;
import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Continent;
import com.lockerroom.resourceservice.sport.model.entity.Country;
import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;
import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SportMapper {

    @Mapping(source = "active", target = "isActive")
    SportResponse toSportResponse(Sport sport);

    ContinentResponse toContinentResponse(Continent continent);

    @Mapping(source = "continent.id", target = "continentId")
    CountryResponse toCountryResponse(Country country);

    /* ── 리그 ── */
    @Mapping(source = "sport.id", target = "sportId")
    @Mapping(source = "country.id", target = "countryId")
    LeagueResponse toLeagueResponse(FootballLeague league);

    @Mapping(source = "sport.id", target = "sportId")
    @Mapping(source = "country.id", target = "countryId")
    LeagueResponse toBaseballLeagueResponse(BaseballLeague league);

    /* ── 팀 ── */
    @Mapping(source = "nameKo", target = "name")
    @Mapping(source = "league.id", target = "leagueId")
    @Mapping(target = "isActive", constant = "true")
    TeamResponse toTeamResponse(FootballTeam team);

    @Mapping(source = "nameKo", target = "name")
    @Mapping(source = "league.id", target = "leagueId")
    @Mapping(target = "isActive", constant = "true")
    TeamResponse toBaseballTeamResponse(BaseballTeam team);
}
