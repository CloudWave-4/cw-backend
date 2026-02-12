package com.cjolivenetworks.CloudWave_Backend.global.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueueEnterRequest {
    private Long productId;
    private Integer quantity;
    private String userId;
}
