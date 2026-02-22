package com.cjolivenetworks.CloudWave_Backend.global.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendMessage(String topic, String key, Object data) {
        try {
            // 1. JSON 문자열로 변환 (직렬화)
            String jsonMessage = objectMapper.writeValueAsString(data);

            log.info("Kafka message sending 시도: Topic={}, Key={}, Data={}", topic, key, jsonMessage);

            // 2. 전송, 결과 확인 (비동기 콜백)
            kafkaTemplate.send(topic, key, jsonMessage).get(800, TimeUnit.MILLISECONDS);
            log.info("Kafka send 성공: topic:{}, key:{}",topic,key);
        } catch (Exception e) {
            log.error("Kafka send 실패: {}", e.getMessage(),e);
            throw new RuntimeException("Kafka publish 실패", e);
        }
    }
}