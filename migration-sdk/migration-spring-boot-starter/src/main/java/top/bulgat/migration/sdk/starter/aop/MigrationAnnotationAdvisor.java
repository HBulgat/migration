package top.bulgat.migration.sdk.starter.aop;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import top.bulgat.migration.sdk.starter.annotation.Migration;

/**
 * Binds methods annotated with {@link Migration} to {@link MigrationInterceptor}.
 */
public class MigrationAnnotationAdvisor extends AbstractPointcutAdvisor {

    private final Advice advice;
    private final Pointcut pointcut;

    /**
     * Creates an annotation advisor.
     *
     * @param interceptor migration interceptor
     */
    public MigrationAnnotationAdvisor(MigrationInterceptor interceptor) {
        this.advice = interceptor;
        this.pointcut = new AnnotationMatchingPointcut(null, Migration.class, true);
    }

    /**
     * Returns the interceptor advice.
     *
     * @return AOP advice
     */
    @Override
    public Advice getAdvice() {
        return advice;
    }

    /**
     * Returns the pointcut matching {@link Migration} methods.
     *
     * @return AOP pointcut
     */
    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }
}
