package com.cjolivenetworks.CloudWave_Backend.global.queue.service;
import com.cjolivenetworks.CloudWave_Backend.global.queue.QueueConstants;
import com.cjolivenetworks.CloudWave_Backend.global.queue.QueueStatus;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueEnterRequest;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueEnterResponse;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final Duration DONE_TTL = Duration.ofMinutes(30); // 작업 DONE/FAIL 이후 결과 보관 TTL 설정
    private final StringRedisTemplate redis;

    // 대기열 티켓 발급_ 진입 성공 여부만
    // * 사용자 고유 token 생성, seq 발급 (INCR), ZSET 에 token 등록, HASH에 token 상태/요청 정보 저장
    // * rank 조회로 현재 position 계산
    public QueueEnterResponse createTicket(QueueEnterRequest request) {

        String token = UUID.randomUUID().toString();

        Long seq = redis.opsForValue().increment(QueueConstants.seqKey());
        if (seq == null) throw new IllegalStateException("seq 초기화 실패");

        redis.opsForZSet().add(QueueConstants.zsetKey(), token, seq);

        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();

        redis.opsForHash().putAll(QueueConstants.ticketKey(token), Map.of(
                "status", QueueStatus.WAITING.name(),
                "userId", request.getUserId() == null ? "anonymous" : request.getUserId(),
                "productId", String.valueOf(request.getProductId()),
                "quantity", String.valueOf(quantity),
                "seq", String.valueOf(seq),
                "createAt", String.valueOf(System.currentTimeMillis())
        ));

        // 현재 순번 계산
        // * rank: 내 앞에 있는 사람 수 (0 based)
        // * position: 사용자에게 보여주는 순번 (1 based)
        Long rank = redis.opsForZSet().rank(QueueConstants.zsetKey(), token);
        long position = (rank == null) ? -1 : rank + 1;

        return QueueEnterResponse.builder()
                .token(token)
                .seq(seq)
                .position(position)
                .build();
    }

    // enqueue 실패 시 롤백
    public void rollbackTicket(String token) {
        redis.opsForZSet().remove(QueueConstants.zsetKey(), token);
        redis.delete(QueueConstants.ticketKey(token));
    }

    // 상태 조회 API
    public QueueStatusResponse getStatus(String token) {
        Map<Object, Object> ticket = redis.opsForHash().entries(QueueConstants.ticketKey(token));

        // 티켓 없을 시 NOT_FOUND
        if (ticket == null || ticket.isEmpty()) {
            return QueueStatusResponse.builder()
                    .token(token)
                    .status("NOT_FOUND")
                    .build();
        }

        String status = String.valueOf(ticket.get("status"));

        Long position = null;
        Long aheadCount = null;

        if (QueueStatus.WAITING.name().equals(status)) {
            // 대기 중인 상태: ZSET에 토큰이 존재해야 함
            Long rank = redis.opsForZSet().rank(QueueConstants.zsetKey(), token);
            // 만약 WAITING인데 rank가 null이라 -> 예외 상황이므로 기본값 처리
            position = (rank == null) ? -1L : rank + 1;
            aheadCount = (rank == null) ? -1L : rank;
        } else {
            // PROCESSING, DONE, FAILED 상태: 이미 대기열에서 빠져나왔으므로 남은 대기 인원은 0
            position = 0L;
            aheadCount = 0L;
        }

        return QueueStatusResponse.builder()
                .token(token)
                .status(status)
                .position(position)
                .aheadCount(aheadCount)
                .result(ticket.get("result") == null ? null : String.valueOf(ticket.get("result")))
                .error(ticket.get("error") == null ? null : String.valueOf(ticket.get("error")))
                .build();
    }

    // 처리 시작
    // * consumer가 kafka 메시지 가져와 작업 수행하기 직전 호출
    // * PROCESSING 되는 순간 ZSET에서 제거
    public void markProcessing(String token) {
        redis.opsForHash().put(QueueConstants.ticketKey(token), "status", QueueStatus.PROCESSING.name());
        redis.opsForHash().put(QueueConstants.ticketKey(token), "updatedAtd", String.valueOf(System.currentTimeMillis()));
        redis.opsForZSet().remove(QueueConstants.zsetKey(), token);
    }

    // 처리 성공
    // * 상태 DONE 저장 + ZSET 상 제거
    // * HASH는 TTL 설정 시간 동안 결과 조회 가능
    public void markDone(String token, String result) {
        redis.opsForHash().put(QueueConstants.ticketKey(token), "status", QueueStatus.DONE.name());
        redis.opsForHash().put(QueueConstants.ticketKey(token), "result", result);
        redis.opsForHash().put(QueueConstants.ticketKey(token), "updatedAt", String.valueOf(System.currentTimeMillis()));

        redis.opsForZSet().remove(QueueConstants.zsetKey(), token);
        redis.expire(QueueConstants.ticketKey(token), DONE_TTL);
    }

    // 처리 실패
    // * 상태 FAILED 저장 + 에러 저장 + ZSET 상 제거
    // * HASH는 TTL 설정 시간 동안 결과 조회 가능
    public void markFailed(String token, String error) {
        redis.opsForHash().put(QueueConstants.ticketKey(token), "status", QueueStatus.FAILED.name());
        redis.opsForHash().put(QueueConstants.ticketKey(token), "error", error);
        redis.opsForHash().put(QueueConstants.ticketKey(token), "updatedAt", String.valueOf(System.currentTimeMillis()));

        redis.opsForZSet().remove(QueueConstants.zsetKey(), token);
        redis.expire(QueueConstants.ticketKey(token), DONE_TTL);
    }

    // consumer 멱등 처리 위한 티켓 raw 조회용
    // * DONE/ FAILED 면 중복 처리 스킵
    public Map<Object, Object> rawTicket(String token) {
        return redis.opsForHash().entries(QueueConstants.ticketKey(token));
    }
}
