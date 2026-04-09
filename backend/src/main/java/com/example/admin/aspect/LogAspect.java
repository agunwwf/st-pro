package com.example.admin.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect      //  告诉Spring切面类
@Component
public class LogAspect {

    //拦截 com.example.admin.controller 包下的所有方法
    @Around("execution(* com.example.admin.controller.*.*(..))")
    public Object recordTimeLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        // 执行原始的 Controller 方法
        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();

        // 打印日志
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("接口调用: {}.{} | 耗时: {} ms", className, methodName, (end - begin));

        return result;
    }
}