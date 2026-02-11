package com.cjolivenetworks.CloudWave_Backend.global.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QueueEnterResponse {
    private String token;
    private long seq;
    private long position;
}
