package com.cjolivenetworks.CloudWave_Backend.global.queue.service;

import com.cjolivenetworks.CloudWave_Backend.global.exception.GlobalErrorCode;
import com.cjolivenetworks.CloudWave_Backend.global.exception.GlobalException;
import com.cjolivenetworks.CloudWave_Backend.global.queue.QueueConstants;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueEnterRequest;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueEnterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

// queue service v2
// queueAdmissionService + QueueService.createTicket  대체
// Lua 를 이용한 redis 병목 완화
@Service
@RequiredArgsConstructor
public class QueueAtomicLuaService {

    private final StringRedisTemplate redis;

    // TODO: 추후 @Value로 빼도 됨
    private static final int MAX_ENQUEUE_PER_SEC = 800;
    private static final long MAX_QUEUE_DEPTH = 100_000;
    private static final long RATE_KEY_TTL_SEC = 2;

    private final DefaultRedisScript<List> enterScript = loadScript();

    private static DefaultRedisScript<List> loadScript() {
        try {
            var res = new ClassPathResource("redis/queue_enter.lua");
            String lua = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            DefaultRedisScript<List> script = new DefaultRedisScript<>();
            script.setScriptText(lua);
            script.setResultType(List.class);
            return script;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load redis/queue_enter.lua", e);
        }
    }

    public QueueEnterResponse enterAtomic(QueueEnterRequest request) {
        String token = UUID.randomUUID().toString();
        String userId = (request.getUserId() == null) ? "anonymous" : request.getUserId();
        String productId = String.valueOf(request.getProductId());
        String quantity = String.valueOf(request.getQuantity() == null ? 1 : request.getQuantity());
        long nowMs = System.currentTimeMillis();
        long nowSec = nowMs / 1000;

        String zsetKey = QueueConstants.zsetKey();
        String seqKey = QueueConstants.seqKey();
        String ticketKey = QueueConstants.ticketKey(token);
        String rlKey = QueueConstants.rateLimitKey(nowSec);

        @SuppressWarnings("unchecked")
        List<String> out = (List<String>) redis.execute(
                enterScript,
                List.of(zsetKey, seqKey, ticketKey, rlKey),
                token,
                String.valueOf(nowMs),
                String.valueOf(MAX_QUEUE_DEPTH),
                String.valueOf(MAX_ENQUEUE_PER_SEC),
                String.valueOf(RATE_KEY_TTL_SEC),
                userId,
                productId,
                quantity
        );

        if (out == null || out.isEmpty()) {
            throw new GlobalException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "Redis Lua returned empty");
        }

        if ("ERR".equals(out.get(0))) {
            String reason = out.size() > 1 ? out.get(1) : "UNKNOWN";
            String detail = out.size() > 2 ? out.get(2) : "";

            if ("QUEUE_FULL".equals(reason)) {
                throw new GlobalException(GlobalErrorCode.TOO_MANY_REQUESTS, "Queue full. depth=" + detail);
            }
            if ("RATE_LIMIT".equals(reason)) {
                throw new GlobalException(GlobalErrorCode.TOO_MANY_REQUESTS, "Rate limited. cnt=" + detail);
            }
            throw new GlobalException(GlobalErrorCode.TOO_MANY_REQUESTS, "Admission rejected: " + reason);
        }

        long seq = Long.parseLong(out.get(1));
        return QueueEnterResponse.builder()
                .token(token)
                .seq(seq)
                // position 안 씀 -> 이후 제거 조치 고려
                .position(-1L)
                .build();
    }

    public void rollbackTicket(String token) {
        redis.opsForZSet().remove(QueueConstants.zsetKey(), token);
        redis.delete(QueueConstants.ticketKey(token));
    }
}
