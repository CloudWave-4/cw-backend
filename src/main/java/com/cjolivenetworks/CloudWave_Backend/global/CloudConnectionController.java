package com.cjolivenetworks.CloudWave_Backend.global;

import com.cjolivenetworks.CloudWave_Backend.global.kafka.TaskProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cloud-test")
@RequiredArgsConstructor
@Slf4j
public class CloudConnectionController {

    private final RedisTemplate<String, String> redisTemplate;
    private final TaskProducer taskProducer;

    @GetMapping("/check/kafka")
    public ResponseEntity<String> checkKafka(){
        String msg = "Testing at " + System.currentTimeMillis();

        try {
            Map<String, Object> kafkaData = new HashMap<>();
            kafkaData.put("source", "CloudConnectionController");
            kafkaData.put("status", "ACTIVE");

            taskProducer.sendMessage("waiting-topic", "cloud-user", kafkaData);
            log.info("MSK 메시지 발송 완료");
        } catch (Exception e) {
            log.error("MSK 연결 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body("Kafka Error: " + e.getMessage());
        }
        return ResponseEntity.ok("Kafka cluster Connected!");

    }

    @GetMapping("/check/redis")
    public ResponseEntity<String> checkAll() {
        String msg = "Testing at " + System.currentTimeMillis();

        // ElastiCache(Redis) 테스트
        try {
            redisTemplate.opsForValue().set("cloud-check", msg);
            Object result = redisTemplate.opsForValue().get("cloud-check");
            log.info("ElastiCache 연결 성공: {}", result);
        } catch (Exception e) {
            log.error("ElastiCache 연결 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body("Redis Error: " + e.getMessage());
        }
        return ResponseEntity.ok("Redis cluster Connected!");
    }
}