package com.whu.shoppingplatform.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
@Order(1)
public class ReadOnlyAspect {

    private static final Logger log = LoggerFactory.getLogger(ReadOnlyAspect.class);

    @Around("@annotation(readOnly)")
    public Object aroundReadOnly(ProceedingJoinPoint pjp, ReadOnly readOnly) throws Throwable {
        try {
            DataSourceRouter.setSlave();
            log.debug("切换到从库: {}", pjp.getSignature().toShortString());
            return pjp.proceed();
        } finally {
            DataSourceRouter.clear();
        }
    }

    @Around("@annotation(transactional)")
    public Object aroundTransactional(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
        if (transactional.readOnly()) {
            try {
                DataSourceRouter.setSlave();
                log.debug("只读事务切换到从库: {}", pjp.getSignature().toShortString());
                return pjp.proceed();
            } finally {
                DataSourceRouter.clear();
            }
        } else {
            try {
                DataSourceRouter.setMaster();
                log.debug("写事务使用主库: {}", pjp.getSignature().toShortString());
                return pjp.proceed();
            } finally {
                DataSourceRouter.clear();
            }
        }
    }
}