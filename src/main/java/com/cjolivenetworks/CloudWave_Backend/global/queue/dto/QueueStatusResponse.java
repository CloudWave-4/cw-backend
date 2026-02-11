package com.cjolivenetworks.CloudWave_Backend.global.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QueueStatusResponse {
    private String token;
    private String status;
    private Long position;
    private Long aheadCount;
    private String result;
    private String error;
}
