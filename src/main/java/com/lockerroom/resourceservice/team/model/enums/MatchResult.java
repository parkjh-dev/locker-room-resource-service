package com.lockerroom.resourceservice.team.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경기 결과.")
public enum MatchResult {
    WIN, DRAW, LOSS
}
