package com.lockerroom.resourceservice.post.mapper;

import com.lockerroom.resourceservice.post.dto.response.PollOptionResponse;
import com.lockerroom.resourceservice.post.dto.response.PollResponse;
import com.lockerroom.resourceservice.post.model.entity.Poll;
import com.lockerroom.resourceservice.post.model.entity.PollOption;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PollMapper {

    PollOptionResponse toOptionResponse(PollOption option);

    default PollResponse toResponse(Poll poll, List<PollOption> options, Long myVoteOptionId) {
        return new PollResponse(
                poll.getQuestion(),
                options.stream().map(this::toOptionResponse).toList(),
                poll.getExpiresAt(),
                poll.getTotalVotes(),
                myVoteOptionId
        );
    }
}
