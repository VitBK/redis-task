package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.util.Set;

@Component
public class JedisRateLimitService implements RateLimitService{

    private final Set<RateLimitRule> rateLimitRules;
    private final JedisCluster jedisCluster;

    public JedisRateLimitService(Set<RateLimitRule> rateLimitRules, JedisCluster jedisCluster) {
        this.rateLimitRules = rateLimitRules;
        this.jedisCluster = jedisCluster;
    }

    @Override
    public boolean shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        boolean shouldLimit = false;
        for (RequestDescriptor rd : requestDescriptors) {
            String key = rd.getClientIp().orElse("") +
                    rd.getAccountId().orElse("") +
                    rd.getRequestType().orElse("");
            RateLimitRule rule = getMatchingRule(rd);
            long count = jedisCluster.incr(key);
            if (count == 1) {
                jedisCluster.expire(key, rule.getTimeInterval().getSeconds());
            }
            shouldLimit = count > rule.getAllowedNumberOfRequests();
        }
        return shouldLimit;
    }

    private RateLimitRule getMatchingRule(RequestDescriptor requestDescriptor) {
        int priority = 99;
        RateLimitRule bestMatch = null;
        for (RateLimitRule rule : rateLimitRules) {
            if (rule.hasEmptyAccountIdAndClientIp()) {
                if (requestTypesArePresentAndEqual(requestDescriptor, rule)) {
                    if (priority > 2) {
                        priority = 2;
                        bestMatch = rule;
                    }
                } else {
                    if (priority > 3) {
                        priority = 3;
                        bestMatch = rule;
                    }
                }
            } else if ((!rule.hasEmptyAccountId() && rule.getAccountId().equals(requestDescriptor.getAccountId())) ||
                    (!rule.hasEmptyClientIp() && rule.getClientIp().equals(requestDescriptor.getClientIp()))) {
                if (requestTypesArePresentAndEqual(requestDescriptor, rule)) {
                    if (priority > 0) {
                        priority = 0;
                        bestMatch = rule;
                    }
                } else {
                    if (priority > 1) {
                        priority = 1;
                        bestMatch = rule;
                    }
                }
            }
        }
        return bestMatch;
    }

    private boolean requestTypesArePresentAndEqual(RequestDescriptor requestDescriptor, RateLimitRule rule) {
        return rule.getRequestType().isPresent() && rule.getRequestType().equals(requestDescriptor.getRequestType());
    }
}
