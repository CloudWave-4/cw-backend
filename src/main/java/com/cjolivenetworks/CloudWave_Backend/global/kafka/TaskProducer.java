package com.cjolivenetworks.CloudWave_Backend.global.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @param topic   메시지 보낼 토픽 명
     * @param key     메시지 키 (보통 userId를 넣어 순서 보장 및 파티셔닝에 활용)
     * @param data    보낼 데이터 (Map, DTO 객체 등)
     */
    public void sendMessage(String topic, String key, Object data) {
        try {
            // 1. JSON 문자열로 변환 (직렬화)
            String jsonMessage = objectMapper.writeValueAsString(data);

            log.info("Kafka 메시지 전송 시도: Topic={}, Key={}, Data={}", topic, key, jsonMessage);

            // 2. 전송, 결과 확인 (비동기 콜백)
            kafkaTemplate.send(topic, key, jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka 전송 성공. Offset: {}", result.getRecordMetadata().offset());
                        } else {
                            log.error("Kafka 전송 실패: {}", ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("메시지 파싱 에러: {}", e.getMessage());
            throw new RuntimeException("메시지 직렬화 실패", e);
        }
    }
}