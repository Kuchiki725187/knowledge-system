package com.knowledge.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final ObjectMapper objectMapper;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, LogAnnotation logAnnotation) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("{} | {} | 方法: {} | 入参: {} | 耗时: {}ms",
                    logAnnotation.module(), logAnnotation.operation(), method,
                    toJson(joinPoint.getArgs()), cost);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            log.error("{} | {} | 方法: {} | 入参: {} | 耗时: {}ms | 异常: {}",
                    logAnnotation.module(), logAnnotation.operation(), method,
                    toJson(joinPoint.getArgs()), cost, e.getMessage());
            throw e;
        }
    }

    private String toJson(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return java.util.Arrays.toString(args);
        }
    }
}
