package com.cjolivenetworks.CloudWave_Backend.global.queue.service;

import com.cjolivenetworks.CloudWave_Backend.global.exception.GlobalErrorCode;
import com.cjolivenetworks.CloudWave_Backend.global.exception.GlobalException;
import com.cjolivenetworks.CloudWave_Backend.global.queue.QueueConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// 1차 진입 제어
@Service
@RequiredArgsConstructor
public class QueueAdmissionService {

    private final StringRedisTemplate redis;

    // 부하 테스트로 p95 처리시간 측정하여 값 스케일링 필요
    private static final int MAX_ENQUEUE_PER_SEC = 800; // 주로 지속 처리량의 70-90%
    private static final long MAX_QUEUE_DEPTH = 100_000; // 허용 최대 대기 시간 기준, 현재: 200msg/s * 300 ms

    // 요청 받을 수 있는가 판단
    // - 불가능으로 판단 시, 429 ERROR (
    public void checkAdmissionOrThrow() {
        // 1. 대기열 길이 상한
        Long depth = redis.opsForZSet().zCard(QueueConstants.zsetKey());
        if (depth != null && depth >= MAX_QUEUE_DEPTH) {
            throw new GlobalException(
                    GlobalErrorCode.TOO_MANY_REQUESTS,
                    "Queue 가 가득 찼습니다. depth=" + depth
            );
        }

        // 2. rate 제한 (초당 enqueue) 통한 단기 폭주 제한
        long nowSec = System.currentTimeMillis() / 1000;
        String rlKey = QueueConstants.rateLimitKey(nowSec);
        Long cnt = redis.opsForValue().increment(rlKey);

        // 첫 증가 시 TLL 설정
        if (cnt != null && cnt == 1L) {
            redis.expire(rlKey, Duration.ofSeconds(2));
        }

        // 제한 초과
        if (cnt != null && cnt > MAX_ENQUEUE_PER_SEC) {
            throw new GlobalException(
                    GlobalErrorCode.TOO_MANY_REQUESTS,
                    "Rate limited. cnt=" + cnt
            );
        }
    }
}
