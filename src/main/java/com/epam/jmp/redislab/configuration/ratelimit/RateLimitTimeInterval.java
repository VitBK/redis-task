package com.epam.jmp.redislab.configuration.ratelimit;

public enum RateLimitTimeInterval {

    MINUTE(60),
    HOUR(3600);

    private final int seconds;

    RateLimitTimeInterval(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }
}
