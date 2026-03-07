package top.bulgat.migration.sdk.starter.aop;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import top.bulgat.migration.sdk.starter.annotation.Migration;

/**
 * 将打上 {@link Migration} 注解的方法绑定到 {@link MigrationInterceptor} 拦截器上。
 */
public class MigrationAnnotationAdvisor extends AbstractPointcutAdvisor {

    private final Advice advice;
    private final Pointcut pointcut;

    /**
     * 创建基于注解的顾问。
     *
     * @param interceptor 迁移拦截器
     */
    public MigrationAnnotationAdvisor(MigrationInterceptor interceptor) {
        this.advice = interceptor;
        this.pointcut = new AnnotationMatchingPointcut(null, Migration.class, true);
    }

    /**
     * 返回拦截器通知。
     *
     * @return AOP 拦截通知
     */
    @Override
    public Advice getAdvice() {
        return advice;
    }

    /**
     * 返回匹配 {@link Migration} 注解方法的切点。
     *
     * @return AOP 切点定义
     */
    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }
}
