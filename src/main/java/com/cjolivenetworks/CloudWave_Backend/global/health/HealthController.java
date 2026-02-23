package com.cjolivenetworks.CloudWave_Backend.global.health;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final StringRedisTemplate redisTemplate;
    private final AdminClient adminClient;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());

        Map<String, Object> redis = checkRedis();
        Map<String, Object> kafka = checkKafka();

        body.put("redis", redis);
        body.put("kafka", kafka);

        boolean up = "UP".equals(redis.get("status")) && "UP".equals(kafka.get("status"));
        body.put("status", up ? "UP" : "DOWN");

        return new ResponseEntity<>(body, up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE);
    }

    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> redis() {
        Map<String, Object> redis = checkRedis();
        boolean up = "UP".equals(redis.get("status"));
        return new ResponseEntity<>(redis, up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE);
    }

    @GetMapping("/kafka")
    public ResponseEntity<Map<String, Object>> kafka() {
        Map<String, Object> kafka = checkKafka();
        boolean up = "UP".equals(kafka.get("status"));
        return new ResponseEntity<>(kafka, up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE);
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> res = new LinkedHashMap<>();
        long start = System.currentTimeMillis();

        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            res.put("status", (pong != null && pong.equalsIgnoreCase("PONG")) ? "UP" : "DOWN");
            res.put("ping", pong);
        } catch (Exception e) {
            res.put("status", "DOWN");
            res.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            res.put("latencyMs", System.currentTimeMillis() - start);
        }
        return res;
    }

    private Map<String, Object> checkKafka() {
        Map<String, Object> res = new LinkedHashMap<>();
        long start = System.currentTimeMillis();

        try {
            DescribeClusterResult r = adminClient.describeCluster();
            String clusterId = r.clusterId().get(5, TimeUnit.SECONDS);
            int nodeCount = r.nodes().get(5, TimeUnit.SECONDS).size();

            res.put("status", "UP");
            res.put("clusterId", clusterId);
            res.put("nodeCount", nodeCount);
        } catch (Exception e) {
            res.put("status", "DOWN");
            res.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            res.put("latencyMs", System.currentTimeMillis() - start);
        }
        return res;
    }
}