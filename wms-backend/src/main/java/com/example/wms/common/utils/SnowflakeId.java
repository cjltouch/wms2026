package com.example.wms.common.utils;

public class SnowflakeId {

    private static final long START_TIMESTAMP = 1480166465631L;

    private static final long SEQUENCE_BIT = 12;
    private static final long WORKER_BIT = 5;
    private static final long DATACENTER_BIT = 5;

    private static final long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);
    private static final long MAX_WORKER_NUM = ~(-1L << WORKER_BIT);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    private static final long WORKER_SHIFT = SEQUENCE_BIT;
    private static final long DATACENTER_SHIFT = SEQUENCE_BIT + WORKER_BIT;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BIT + WORKER_BIT + DATACENTER_BIT;

    private final long datacenterId;
    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private static final SnowflakeId INSTANCE = new SnowflakeId(1L, 1L);

    private SnowflakeId(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId is invalid");
        }
        if (workerId > MAX_WORKER_NUM || workerId < 0) {
            throw new IllegalArgumentException("workerId is invalid");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public static SnowflakeId getInstance() {
        return INSTANCE;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
