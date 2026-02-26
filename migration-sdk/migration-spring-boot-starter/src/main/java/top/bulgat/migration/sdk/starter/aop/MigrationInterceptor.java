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
     * Creates a migration interceptor.
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
     * Intercepts method invocation and delegates execution to {@link MigrationClient}.
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
                        .timeout(migrationProperties.toSdkProperties().getDefaultTimeout())
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
                : invokeFallbackFunction(fallbackMethod.target(), fallbackMethod.method());

        return client.wrap(oldInvoker, newInvoker, fallbackInvoker, paramHandler).apply(args);
    }

    /**
     * Wraps reflected method into a callable function.
     *
     * @param target target bean
     * @param method reflected method
     * @return invocation function
     */
    private Function<Object[], Object> invokeFunction(Object target, Method method) {
        return invocationArgs -> invoke(target, method, invocationArgs);
    }

    /**
     * Wraps fallback method into a callable function with appended exception argument.
     *
     * @param target target bean
     * @param method fallback method
     * @return fallback invocation function
     */
    private BiFunction<Object[], Exception, Object> invokeFallbackFunction(Object target, Method method) {
        return (invocationArgs, ex) -> {
            Object[] finalArgs = new Object[invocationArgs.length + 1];
            System.arraycopy(invocationArgs, 0, finalArgs, 0, invocationArgs.length);
            finalArgs[finalArgs.length - 1] = ex;
            return invoke(target, method, finalArgs);
        };
    }

    /**
     * Invokes target method via reflection.
     *
     * @param target target bean
     * @param method method to invoke
     * @param args method arguments
     * @return invocation result
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
     * Resolves descriptor in methodName / beanName#methodName / beanName.methodName format.
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

        if (splitIndex < 0) {
            Method method = resolveMethod(defaultTarget.getClass(), descriptor, entryTypes, fallbackMethod);
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

        Method method = resolveMethod(bean.getClass(), methodName, entryTypes, fallbackMethod);
        return new ResolvedMethod(bean, method);
    }

    /**
     * Resolves method by signature and fallback constraints.
     */
    private Method resolveMethod(
            Class<?> targetClass,
            String methodName,
            Class<?>[] entryTypes,
            boolean fallbackMethod) {
        if (!fallbackMethod) {
            Method method = ReflectionUtils.findMethod(targetClass, methodName, entryTypes);
            if (method != null) {
                return method;
            }
        }

        Method[] methods = ReflectionUtils.getAllDeclaredMethods(targetClass);
        int expectedLength = fallbackMethod ? entryTypes.length + 1 : entryTypes.length;
        for (Method candidate : methods) {
            if (!Objects.equals(candidate.getName(), methodName)) {
                continue;
            }
            if (candidate.getParameterCount() != expectedLength) {
                continue;
            }
            if (!matchEntryParams(candidate.getParameterTypes(), entryTypes)) {
                continue;
            }
            if (fallbackMethod) {
                Class<?> tailType = candidate.getParameterTypes()[expectedLength - 1];
                if (!Exception.class.isAssignableFrom(tailType) && !Throwable.class.isAssignableFrom(tailType)) {
                    continue;
                }
            }
            return candidate;
        }
        throw new IllegalStateException("method not found: " + methodName + " in " + targetClass.getName());
    }

    /**
     * Checks whether candidate leading parameters are compatible with entry parameters.
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
     * Resolves param handler instance from Spring context or reflection.
     *
     * @param handlerClass handler class from annotation
     * @return param handler instance
     */
    private ParamHandler resolveParamHandler(Class<? extends ParamHandler> handlerClass) {
        if (handlerClass == null || handlerClass == ParamHandler.class) {
            return args -> Collections.emptyMap();
        }
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(handlerClass);
            } catch (Exception ignored) {
                // Fall back to reflection-based construction.
            }
        }
        try {
            return handlerClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("cannot create param handler: " + handlerClass.getName(), ex);
        }
    }

    /**
     * Resolves per-method executor and caches it by method signature.
     *
     * @param method entry method
     * @param migration migration annotation
     * @return executor service
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
     * Injects Spring application context.
     *
     * @param applicationContext spring context
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Shuts down all internally created executors.
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
