package com.cjolivenetworks.CloudWave_Backend.global.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "health-check-topic", groupId = "test-group")
    public void consume(String message) {
        try {
            // JSON 문자열 역직렬화
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            log.info("Kafka로부터 받은 데이터: status={}, time={}",
                    data.get("status"), data.get("timestamp"));
        } catch (Exception e) {
            log.error("메시지 파싱 에러: {}", e.getMessage());
        }
    }
}