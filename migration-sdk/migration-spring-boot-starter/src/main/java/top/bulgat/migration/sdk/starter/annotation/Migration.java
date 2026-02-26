package top.bulgat.migration.sdk.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import top.bulgat.migration.sdk.core.function.ParamHandler;

/**
 * 迁移注解：声明当前方法由 SDK 接管新旧逻辑路由。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Migration {

    /**
     * 迁移任务唯一标识。
     */
    String key();

    /**
     * 旧逻辑方法描述：支持 methodName 或 beanName#methodName。
     */
    String oldMethod();

    /**
     * 新逻辑方法描述：支持 methodName 或 beanName#methodName。
     */
    String newMethod();

    /**
     * 降级方法描述（可选）：签名为“原参数 + Exception/Throwable”。
     */
    String fallBackMethod() default "";

    /**
     * 参数处理器类型。
     */
    Class<? extends ParamHandler> paramHandler() default ParamHandler.class;

    /**
     * 核心线程数。
     */
    int corePoolSize() default 2;

    /**
     * 最大线程数。
     */
    int maxPoolSize() default 10;

    /**
     * 队列容量。
     */
    int queueCapacity() default 100;

    /**
     * 线程名前缀。
     */
    String threadNamePrefix() default "migration-";
}
