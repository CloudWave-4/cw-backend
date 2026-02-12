package com.cjolivenetworks.CloudWave_Backend.global.queue;

public class QueueConstants {
    public static final String QUEUE_NAME= "main";
    public static final String QUEUE_TOPIC= "queue-topic";

    public static String seqKey() { return "queue:" + QUEUE_NAME + ":seq"; }
    public static String zsetKey() { return "queue:" + QUEUE_NAME + ":zset"; }
    public static String ticketKey(String token) { return "queue:" + QUEUE_NAME + ":ticket:" + token; }
    public static String rateLimitKey(long epochSecond) { return "queue:" + QUEUE_NAME + ":rl:" + epochSecond; }
}
