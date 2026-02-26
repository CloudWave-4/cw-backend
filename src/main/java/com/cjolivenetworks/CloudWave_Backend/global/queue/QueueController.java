package com.cjolivenetworks.CloudWave_Backend.global.queue;

import com.cjolivenetworks.CloudWave_Backend.global.kafka.TaskProducer;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueEnterRequest;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueEnterResponse;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueStatusResponse;
import com.cjolivenetworks.CloudWave_Backend.global.queue.service.QueueAdmissionService;
import com.cjolivenetworks.CloudWave_Backend.global.queue.service.QueueAtomicLuaService;
import com.cjolivenetworks.CloudWave_Backend.global.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

//    private final QueueAdmissionService admissionService;
    private final QueueAtomicLuaService atomicLuaService;
    private final QueueService queueService;
    private final TaskProducer taskProducer;

    @PostMapping("/enter")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public QueueEnterResponse enter(@RequestBody QueueEnterRequest request) {
//        // 1) 진입 폭주 억제
//        admissionService.checkAdmissionOrThrow();
        QueueEnterResponse response = atomicLuaService.enterAtomic(request);

//        // 2) 티켓 생성(순번,상태)
//        QueueEnterResponse resp = queueService.createTicket(request);

        // 3) Kafka enqueue (메인 큐)
        Map<String, Object> msg = new HashMap<>();
        msg.put("token", response.getToken());
        msg.put("userId", request.getUserId());
        msg.put("productId", request.getProductId());
        msg.put("quantity", request.getQuantity() == null ? 1 : request.getQuantity());

        String key = request.getUserId() == null ? response.getToken() : request.getUserId();

        try {
            taskProducer.sendMessage(QueueConstants.QUEUE_TOPIC, key, msg);
        } catch (Exception e) {
            // Kafka enqueue 실패 시 롤백
            queueService.rollbackTicket(response.getToken());
            throw e;
        }

        return response;
    }

    @GetMapping("/{token}")
    public QueueStatusResponse status(@PathVariable String token) {
        return queueService.getStatus(token);
    }

    // SSE 실시간 조회
    @GetMapping("/{token}/stream")
    public SseEmitter stream(@PathVariable String token) {
        SseEmitter emitter = new SseEmitter(60_000L);

        new Thread(() -> {
            try {
                for (int i = 0; i < 60; i++) {
                    QueueStatusResponse status = queueService.getStatus(token);
                    emitter.send(SseEmitter.event().name("status").data(status));

                    if ("DONE".equals(status.getStatus())
                            || "FAILED".equals(status.getStatus())
                            || "NOT_FOUND".equals(status.getStatus())) {
                        emitter.complete();
                        return;
                    }
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

}