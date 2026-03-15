package top.bulgat.migration.sdk.starter.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import top.bulgat.migration.sdk.core.client.MigrationClient;
import top.bulgat.migration.sdk.core.function.ParamHandler;
import top.bulgat.migration.sdk.core.model.MigrationConfig;
import top.bulgat.migration.sdk.core.spi.ConfigClient;
import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;
import top.bulgat.migration.sdk.core.spi.GrayscaleMatcher;
import top.bulgat.migration.sdk.core.strategy.MigrationStrategyRegistry;
import top.bulgat.migration.sdk.starter.annotation.Migration;
import top.bulgat.migration.sdk.starter.config.MigrationProperties;

/**
 * 迁移注解方法拦截器。
 * <p>
 * 负责解析 old/new/fallback 方法描述符，并委托 {@link MigrationClient}
 * 按迁移状态执行路由。
 */
public class MigrationInterceptor implements MethodInterceptor, ApplicationContextAware, DisposableBean {

    private final ConfigClient configClient;
    private final DiffServiceCaller diffServiceCaller;
    private final GrayscaleMatcher grayscaleMatcher;
    private final MigrationStrategyRegistry strategyRegistry;
    private final MigrationProperties migrationProperties;
    private final Map<String, ExecutorService> executorCache = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    /**
     * 创建迁移拦截器。
     */
    public MigrationInterceptor(
            ConfigClient configClient,
            DiffServiceCaller diffServiceCaller,
            GrayscaleMatcher grayscaleMatcher,
            MigrationStrategyRegistry strategyRegistry,
            MigrationProperties migrationProperties) {
        this.configClient = configClient;
        this.diffServiceCaller = diffServiceCaller;
        this.grayscaleMatcher = grayscaleMatcher;
        this.strategyRegistry = strategyRegistry;
        this.migrationProperties = migrationProperties;
    }

    /**
     * 拦截方法调用，并将执行委托给 {@link MigrationClient}。
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method entryMethod = invocation.getMethod();
        Migration migration = AnnotationUtils.findAnnotation(entryMethod, Migration.class);
        if (migration == null) {
            return invocation.proceed();
        }

        Object target = invocation.getThis();
        Object[] args = invocation.getArguments();
        if (target == null) {
            return invocation.proceed();
        }

        ResolvedMethod oldMethod = resolveTargetMethod(
                target,
                migration.oldMethod(),
                entryMethod.getParameterTypes(),
                false);
        ResolvedMethod newMethod = resolveTargetMethod(
                target,
                migration.newMethod(),
                entryMethod.getParameterTypes(),
                false);
        ResolvedMethod fallbackMethod = migration.fallBackMethod().isBlank()
                ? null
                : resolveTargetMethod(target, migration.fallBackMethod(), entryMethod.getParameterTypes(), true);

        ParamHandler paramHandler = resolveParamHandler(migration.paramHandler());
        ExecutorService executorService = resolveExecutor(entryMethod, migration);

        MigrationClient client = new MigrationClient(
                MigrationConfig.builder()
                        .migrationKey(migration.key())
                        .build(),
                configClient,
                diffServiceCaller,
                grayscaleMatcher,
                strategyRegistry,
                executorService);

        Function<Object[], Object> oldInvoker = invokeFunction(oldMethod.target(), oldMethod.method());
        Function<Object[], Object> newInvoker = invokeFunction(newMethod.target(), newMethod.method());
        BiFunction<Object[], Exception, Object> fallbackInvoker = fallbackMethod == null
                ? null
                : invokeFallbackFunction(fallbackMethod.target(), fallbackMethod.method(), entryMethod.getParameterCount());

        return client.wrap(oldInvoker, newInvoker, fallbackInvoker, paramHandler).apply(args);
    }

    /**
     * 将反射方法包装成可调用的函数。
     *
     * @param target 目标 Bean
     * @param method 反射的方法
     * @return 调用函数
     */
    private Function<Object[], Object> invokeFunction(Object target, Method method) {
        return invocationArgs -> invoke(target, method, invocationArgs);
    }

    /**
     * 将降级方法包装成可调用的函数，并在参数末尾追加异常对象。
     *
     * @param target 目标 Bean
     * @param method 降级方法
     * @return 降级调用函数
     */
    private BiFunction<Object[], Exception, Object> invokeFallbackFunction(Object target, Method method, int entryParamCount) {
        return (invocationArgs, ex) -> {
            if (method.getParameterCount() == entryParamCount + 1) {
                Object[] finalArgs = new Object[invocationArgs.length + 1];
                System.arraycopy(invocationArgs, 0, finalArgs, 0, invocationArgs.length);
                finalArgs[finalArgs.length - 1] = ex;
                return invoke(target, method, finalArgs);
            }
            // 如果降级方法没有异常参数，则直接使用原参数调用。
            return invoke(target, method, invocationArgs);
        };
    }

    /**
     * 通过反射调用目标方法。
     *
     * @param target 目标 Bean
     * @param method 待调用的方法
     * @param args   方法入参
     * @return 调用结果
     */
    private Object invoke(Object target, Method method, Object[] args) {
        try {
            ReflectionUtils.makeAccessible(method);
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(targetException);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 解析方法描述符，支持格式：方法名、beanName#methodName 或 beanName.methodName。
     */
    private ResolvedMethod resolveTargetMethod(
            Object defaultTarget,
            String methodDescriptor,
            Class<?>[] entryTypes,
            boolean fallbackMethod) {
        String descriptor = methodDescriptor == null ? "" : methodDescriptor.trim();
        if (descriptor.isBlank()) {
            throw new IllegalStateException("method descriptor is blank");
        }

        int splitIndex = descriptor.indexOf('#');
        if (splitIndex < 0 && descriptor.contains(".")) {
            splitIndex = descriptor.lastIndexOf('.');
        }

        Class<?> actualTargetClass = AopUtils.getTargetClass(defaultTarget);
        if (splitIndex < 0) {
            Method method = resolveMethod(actualTargetClass, descriptor, entryTypes, fallbackMethod);
            return new ResolvedMethod(defaultTarget, method);
        }

        if (splitIndex == 0 || splitIndex == descriptor.length() - 1) {
            throw new IllegalStateException("invalid method descriptor: " + methodDescriptor);
        }

        String beanName = descriptor.substring(0, splitIndex).trim();
        String methodName = descriptor.substring(splitIndex + 1).trim();
        if (beanName.isBlank() || methodName.isBlank()) {
            throw new IllegalStateException("invalid method descriptor: " + methodDescriptor);
        }
        if (applicationContext == null) {
            throw new IllegalStateException("applicationContext is not initialized");
        }

        Object bean;
        try {
            bean = applicationContext.getBean(beanName);
        } catch (Exception ex) {
            throw new IllegalStateException("bean not found: " + beanName, ex);
        }

        Class<?> beanClass = AopUtils.getTargetClass(bean);
        Method method = resolveMethod(beanClass, methodName, entryTypes, fallbackMethod);
        return new ResolvedMethod(bean, method);
    }

    /**
     * 根据方法签名和是否为降级方法的约束条件来解析具体的方法。
     */
    private Method resolveMethod(
            Class<?> targetClass,
            String methodName,
            Class<?>[] entryTypes,
            boolean fallbackMethod) {
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(targetClass);
        // 首先尝试精确匹配签名（不带异常参数）。
        for (Method candidate : methods) {
            if (!Objects.equals(candidate.getName(), methodName)) {
                continue;
            }
            if (candidate.getParameterCount() != entryTypes.length) {
                continue;
            }
            if (matchEntryParams(candidate.getParameterTypes(), entryTypes)) {
                return candidate;
            }
        }

        // 如果没有精确匹配到原签名且是降级方法，尝试查找带异常参数的签名。
        if (fallbackMethod) {
            for (Method candidate : methods) {
                if (!Objects.equals(candidate.getName(), methodName)) {
                    continue;
                }
                if (candidate.getParameterCount() != entryTypes.length + 1) {
                    continue;
                }
                if (!matchEntryParams(candidate.getParameterTypes(), entryTypes)) {
                    continue;
                }
                Class<?> tailType = candidate.getParameterTypes()[entryTypes.length];
                if (Throwable.class.isAssignableFrom(tailType)) {
                    return candidate;
                }
            }
        }

        throw new IllegalStateException(String.format(
                "method not found: %s with params %s in %s (fallbackMode=%b)",
                methodName, java.util.Arrays.toString(entryTypes), targetClass.getName(), fallbackMethod));
    }

    /**
     * 检查候选方法的参数类型是否与入口方法的参数兼容。
     */
    private boolean matchEntryParams(Class<?>[] candidateTypes, Class<?>[] entryTypes) {
        if (entryTypes.length > candidateTypes.length) {
            return false;
        }
        for (int i = 0; i < entryTypes.length; i++) {
            if (!ClassUtils.isAssignable(candidateTypes[i], entryTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从Spring 上下文中解析参数处理器实例，或者通过反射创建实例。
     *
     * @param handlerClass 注解中指定的参数处理器类
     * @return 参数处理器实例
     */
    private ParamHandler resolveParamHandler(Class<? extends ParamHandler> handlerClass) {
        if (handlerClass == null || handlerClass == ParamHandler.class) {
            return args -> Collections.emptyMap();
        }
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(handlerClass);
            } catch (Exception ignored) {
                // 回退到基于反射的实例化方式。
            }
        }
        try {
            return handlerClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("cannot create param handler: " + handlerClass.getName(), ex);
        }
    }

    /**
     * 为每个方法解析所需的线程池，并根据方法签名进行缓存。
     *
     * @param method    入口方法
     * @param migration 迁移注解
     * @return 线程池服务
     */
    private ExecutorService resolveExecutor(Method method, Migration migration) {
        String key = method.toGenericString();
        return executorCache.computeIfAbsent(key, k -> {
            int corePoolSize = Math.max(1, migration.corePoolSize());
            int maxPoolSize = Math.max(corePoolSize, migration.maxPoolSize());
            int queueCapacity = Math.max(1, migration.queueCapacity());
            String prefix = migration.threadNamePrefix() == null || migration.threadNamePrefix().isBlank()
                    ? "migration-"
                    : migration.threadNamePrefix();
            AtomicInteger index = new AtomicInteger(1);
            return new ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    60,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    runnable -> {
                        Thread thread = new Thread(runnable);
                        thread.setDaemon(true);
                        thread.setName(prefix + index.getAndIncrement());
                        return thread;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy());
        });
    }

    /**
     * 注入Spring应用上下文。
     *
     * @param applicationContext Spring 应用上下文
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 关闭内部创建的所有线程池。
     */
    @Override
    public void destroy() {
        for (ExecutorService executor : executorCache.values()) {
            executor.shutdown();
        }
    }

    private record ResolvedMethod(Object target, Method method) {
    }
}
