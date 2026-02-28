package top.bulgat.migration.admin.infrastructure.config;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 全局详细日志切面，拦截所有业务逻辑的组件并打印其出入参
 */
@Aspect
@Component
@Slf4j
public class GlobalLoggingAspect {

    /**
     * 拦截 admin 包下所有类的所有方法，但排除当前配置包自身（避免循环或代理配置类干扰）
     */
    @Pointcut("execution(* top.bulgat.migration.admin..*(..)) && !within(top.bulgat.migration.admin.infrastructure.config..*) && !within(top.bulgat.migration.admin.domain.model..*)")
    public void allMethods() {
    }

    @Around("allMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("============== START EXECUTION ==============");
        log.info(">>>> Method: [{}] started.", methodName);

        try {
            // 参数可能无法序列化，先尝试转JSON，失败则用Arrays.toString
            log.info(">>>> Arguments: {}", JSON.toJSONString(args));
        } catch (Exception e) {
            log.info(">>>> Arguments (raw): {}", Arrays.toString(args));
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - start;

            log.info("<<<< Method: [{}] completed successfully in {} ms.", methodName, elapsedTime);
            try {
                log.info("<<<< Return value: {}", JSON.toJSONString(result));
            } catch (Exception e) {
                log.info("<<<< Return value (raw): {}", result);
            }
            log.info("============== END EXECUTION ==============\n");
            return result;
        } catch (Throwable e) {
            long elapsedTime = System.currentTimeMillis() - start;
            log.error("<<<< Method: [{}] threw exception in {} ms.", methodName, elapsedTime);
            log.error("<<<< Exception message: {}", e.getMessage(), e);
            log.info("============== END EXECUTION WITH ERROR ==============\n");
            throw e;
        }
    }
}
