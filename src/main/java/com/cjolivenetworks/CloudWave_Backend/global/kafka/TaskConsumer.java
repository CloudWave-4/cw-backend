package com.cjolivenetworks.CloudWave_Backend.global.kafka;

import com.cjolivenetworks.CloudWave_Backend.global.queue.service.QueueService;
import com.cjolivenetworks.CloudWave_Backend.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskConsumer {

    private final ObjectMapper objectMapper;
    private final QueueService queueService;
    private final ProductService productService;

    @KafkaListener(topics = "health-check-topic", groupId = "test-group")
    public void consumeHealth(String message) {
        try {
            // JSON 문자열 역직렬화
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            log.info("Kafka로부터 받은 데이터: status={}, time={}",
                    data.get("status"), data.get("timestamp"));
        } catch (Exception e) {
            log.error("메시지 파싱 에러: {}", e.getMessage());
        }
    }

    @KafkaListener(topics="queue-topic")
    public void consumeQueue(String message,Acknowledgment acknowledgment) {
        try{
            Map<String,Object> data= objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});

            String token=String.valueOf(data.get("token"));
            Long productId=Long.valueOf(String.valueOf(data.get("productId")));
            int quantity=Integer.parseInt(String.valueOf(data.getOrDefault("quantity",1)));

            // 최소 멱등 처리 : DONE/ FAILED 일 시 스킵
            Map<Object,Object>ticket=queueService.rawTicket(token);
            if(ticket!=null&&!ticket.isEmpty()){
                String string=String.valueOf(ticket.get("status"));
                if("DONE".equals(string)||"FAILED".equals(string)){
                    acknowledgment.acknowledge();
                    return;
                }
            }

            // 1. 상태: PROCESSING
            queueService.markProcessing(token);

            // 2. 처리: 재고 증가
            productService.increaseStock(productId,Math.max(quantity,1));

            // 3. 상태: DONE, ZSET 제거
            queueService.markDone(token,"OK stock++ productID="+productId+",quantity="+Math.max(quantity,1));

            acknowledgment.acknowledge();
        } catch (Exception e){
            log.error("Queue task 처리 실패: {}",e.getMessage(),e);

            // 실패 상태 기록
            try {
                Map<String,Object> data= objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
                String token=String.valueOf(data.get("token"));
                queueService.markFailed(token,e.getMessage());
            }catch (Exception ignore){}

            // KafkaConfig의 ErrorHandler가 재시도 후 DLT로 전송
            throw new RuntimeException(e);
        }
    }
}