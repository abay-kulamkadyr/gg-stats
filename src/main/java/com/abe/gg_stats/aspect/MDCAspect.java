package com.abe.gg_stats.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MDCAspect {

	@Around("within(@org.springframework.stereotype.Service *)")
	public Object logServiceCalls(ProceedingJoinPoint pjp) throws Throwable {
		try {
			MDC.put("serviceName", pjp.getTarget().getClass().getSimpleName());
			MDC.put("operationName", pjp.getSignature().getName());
			return pjp.proceed();
		}
		finally {
			MDC.clear();
		}
	}

}
