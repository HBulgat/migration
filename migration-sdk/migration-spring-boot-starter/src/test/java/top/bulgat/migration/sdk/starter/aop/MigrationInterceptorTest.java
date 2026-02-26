package top.bulgat.migration.sdk.starter.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import top.bulgat.migration.sdk.core.function.ParamHandler;
import top.bulgat.migration.sdk.core.model.DiffRequest;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.MigrationConfig;
import top.bulgat.migration.sdk.core.model.MigrationStatus;
import top.bulgat.migration.sdk.core.spi.ConfigClient;
import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;
import top.bulgat.migration.sdk.core.strategy.MigrationStrategyRegistry;
import top.bulgat.migration.sdk.starter.annotation.Migration;
import top.bulgat.migration.sdk.starter.config.MigrationProperties;

class MigrationInterceptorTest {

    @Test
    void invoke_shouldRouteByStatus() {
        DemoService.OLD_COUNT.set(0);
        DemoService.NEW_COUNT.set(0);

        FakeConfigClient configClient = new FakeConfigClient();
        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
        MigrationInterceptor interceptor = new MigrationInterceptor(
                configClient,
                diffServiceCaller,
                new top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                new MigrationProperties());
        interceptor.setApplicationContext(new StaticApplicationContext());

        MigrationAnnotationAdvisor advisor = new MigrationAnnotationAdvisor(interceptor);
        ProxyFactory factory = new ProxyFactory(new DemoService());
        factory.addAdvisor(advisor);
        DemoService proxy = (DemoService) factory.getProxy();

        configClient.status = MigrationStatus.OLD.getCode();
        String oldResult = proxy.query("1001");
        assertEquals("old:1001", oldResult);

        configClient.status = MigrationStatus.GO_LIVE_ALL.getCode();
        String newResult = proxy.query("1001");
        assertEquals("new:1001", newResult);

        assertEquals(2, DemoService.OLD_COUNT.get());
        assertEquals(1, DemoService.NEW_COUNT.get());
        assertEquals(1, diffServiceCaller.requests.size());
    }

    @Test
    void invoke_shouldUseReflectionCreatedParamHandlerForGrayscale() {
        ParamHandlerDemoService.OLD_COUNT.set(0);
        ParamHandlerDemoService.NEW_COUNT.set(0);

        GrayscaleConfig whitelistRule = GrayscaleConfig.builder()
                .ruleType("WHITELIST")
                .ruleValue("[\"9001\"]")
                .enable(true)
                .build();

        FakeConfigClient configClient = new FakeConfigClient();
        configClient.status = MigrationStatus.GO_LIVE_GRAY.getCode();
        configClient.rules = List.of(whitelistRule);
        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
        MigrationInterceptor interceptor = new MigrationInterceptor(
                configClient,
                diffServiceCaller,
                new top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                new MigrationProperties());

        StaticApplicationContext context = new StaticApplicationContext();
        context.refresh();
        interceptor.setApplicationContext(context);

        MigrationAnnotationAdvisor advisor = new MigrationAnnotationAdvisor(interceptor);
        ProxyFactory factory = new ProxyFactory(new ParamHandlerDemoService());
        factory.addAdvisor(advisor);
        ParamHandlerDemoService proxy = (ParamHandlerDemoService) factory.getProxy();

        String result = proxy.query("9001");

        assertEquals("new:9001", result);
        assertEquals(0, ParamHandlerDemoService.OLD_COUNT.get());
        assertEquals(1, ParamHandlerDemoService.NEW_COUNT.get());
        assertEquals(0, diffServiceCaller.requests.size());

        context.close();
    }

    @Test
    void invoke_shouldResolveExternalBeanMethodByDotName() {
        OldService.COUNT.set(0);
        NewService.COUNT.set(0);

        FakeConfigClient configClient = new FakeConfigClient();
        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
        MigrationInterceptor interceptor = new MigrationInterceptor(
                configClient,
                diffServiceCaller,
                new top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                new MigrationProperties());

        StaticApplicationContext context = new StaticApplicationContext();
        context.registerSingleton("oldServiceBean", OldService.class);
        context.registerSingleton("newServiceBean", NewService.class);
        context.refresh();
        interceptor.setApplicationContext(context);

        MigrationAnnotationAdvisor advisor = new MigrationAnnotationAdvisor(interceptor);
        ProxyFactory factory = new ProxyFactory(new CrossBeanDotDemoService());
        factory.addAdvisor(advisor);
        CrossBeanDotDemoService proxy = (CrossBeanDotDemoService) factory.getProxy();

        configClient.status = MigrationStatus.OLD.getCode();
        String oldResult = proxy.query("2101");
        assertEquals("external-old:2101", oldResult);

        configClient.status = MigrationStatus.GO_LIVE_ALL.getCode();
        String newResult = proxy.query("2101");
        assertEquals("external-new:2101", newResult);

        assertEquals(2, OldService.COUNT.get());
        assertEquals(1, NewService.COUNT.get());
        assertEquals(1, diffServiceCaller.requests.size());

        context.close();
    }

    @Test
    void invoke_shouldResolveExternalBeanMethodByName() {
        OldService.COUNT.set(0);
        NewService.COUNT.set(0);

        FakeConfigClient configClient = new FakeConfigClient();
        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
        MigrationInterceptor interceptor = new MigrationInterceptor(
                configClient,
                diffServiceCaller,
                new top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                new MigrationProperties());

        StaticApplicationContext context = new StaticApplicationContext();
        context.registerSingleton("oldServiceBean", OldService.class);
        context.registerSingleton("newServiceBean", NewService.class);
        context.refresh();
        interceptor.setApplicationContext(context);

        MigrationAnnotationAdvisor advisor = new MigrationAnnotationAdvisor(interceptor);
        ProxyFactory factory = new ProxyFactory(new CrossBeanDemoService());
        factory.addAdvisor(advisor);
        CrossBeanDemoService proxy = (CrossBeanDemoService) factory.getProxy();

        configClient.status = MigrationStatus.OLD.getCode();
        String oldResult = proxy.query("2001");
        assertEquals("external-old:2001", oldResult);

        configClient.status = MigrationStatus.GO_LIVE_ALL.getCode();
        String newResult = proxy.query("2001");
        assertEquals("external-new:2001", newResult);

        assertEquals(2, OldService.COUNT.get());
        assertEquals(1, NewService.COUNT.get());
        assertEquals(1, diffServiceCaller.requests.size());

        context.close();
    }

    @Test
    void invoke_shouldFallbackToOldWithoutDuplicateCallWhenNewThrows() {
        NewFailNoFallbackDemoService.OLD_COUNT.set(0);
        NewFailNoFallbackDemoService.NEW_FAIL_COUNT.set(0);

        FakeConfigClient configClient = new FakeConfigClient();
        configClient.status = MigrationStatus.GO_LIVE_ALL.getCode();
        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
        MigrationInterceptor interceptor = new MigrationInterceptor(
                configClient,
                diffServiceCaller,
                new top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                new MigrationProperties());
        interceptor.setApplicationContext(new StaticApplicationContext());

        MigrationAnnotationAdvisor advisor = new MigrationAnnotationAdvisor(interceptor);
        ProxyFactory factory = new ProxyFactory(new NewFailNoFallbackDemoService());
        factory.addAdvisor(advisor);
        NewFailNoFallbackDemoService proxy = (NewFailNoFallbackDemoService) factory.getProxy();

        String result = proxy.query("7001");

        assertEquals("old:7001", result);
        assertEquals(1, NewFailNoFallbackDemoService.OLD_COUNT.get());
        assertEquals(1, NewFailNoFallbackDemoService.NEW_FAIL_COUNT.get());
        assertEquals(0, diffServiceCaller.requests.size());
    }

    @Test
    void invoke_shouldResolveExternalFallbackMethodByDotNameAndThrowable() {
        OldService.COUNT.set(0);
        NewService.FAIL_COUNT.set(0);
        ThrowableFallbackService.COUNT.set(0);

        FakeConfigClient configClient = new FakeConfigClient();
        configClient.status = MigrationStatus.GO_LIVE_ALL.getCode();
        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
        MigrationInterceptor interceptor = new MigrationInterceptor(
                configClient,
                diffServiceCaller,
                new top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                new MigrationProperties());

        StaticApplicationContext context = new StaticApplicationContext();
        context.registerSingleton("oldServiceBean", OldService.class);
        context.registerSingleton("newServiceBean", NewService.class);
        context.registerSingleton("throwableFallbackServiceBean", ThrowableFallbackService.class);
        context.refresh();
        interceptor.setApplicationContext(context);

        MigrationAnnotationAdvisor advisor = new MigrationAnnotationAdvisor(interceptor);
        ProxyFactory factory = new ProxyFactory(new CrossBeanThrowableFallbackDemoService());
        factory.addAdvisor(advisor);
        CrossBeanThrowableFallbackDemoService proxy = (CrossBeanThrowableFallbackDemoService) factory.getProxy();

        String result = proxy.query("3201");

        assertEquals("throwable-fallback:3201", result);
        assertEquals(1, OldService.COUNT.get());
        assertEquals(1, NewService.FAIL_COUNT.get());
        assertEquals(1, ThrowableFallbackService.COUNT.get());
        assertEquals(0, diffServiceCaller.requests.size());

        context.close();
    }

    @Test
    void invoke_shouldResolvePrivateFallbackMethodOnSameBean() {
        PrivateFallbackDemoService.OLD_COUNT.set(0);
        PrivateFallbackDemoService.NEW_FAIL_COUNT.set(0);
        PrivateFallbackDemoService.FALLBACK_COUNT.set(0);

        FakeConfigClient configClient = new FakeConfigClient();
        configClient.status = MigrationStatus.GO_LIVE_ALL.getCode();
        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
        MigrationInterceptor interceptor = new MigrationInterceptor(
                configClient,
                diffServiceCaller,
                new top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                new MigrationProperties());
        interceptor.setApplicationContext(new StaticApplicationContext());

        MigrationAnnotationAdvisor advisor = new MigrationAnnotationAdvisor(interceptor);
        ProxyFactory factory = new ProxyFactory(new PrivateFallbackDemoService());
        factory.addAdvisor(advisor);
        PrivateFallbackDemoService proxy = (PrivateFallbackDemoService) factory.getProxy();

        String result = proxy.query("9101");

        assertEquals("private-fallback:9101", result);
        assertEquals(1, PrivateFallbackDemoService.OLD_COUNT.get());
        assertEquals(1, PrivateFallbackDemoService.NEW_FAIL_COUNT.get());
        assertEquals(1, PrivateFallbackDemoService.FALLBACK_COUNT.get());
        assertEquals(0, diffServiceCaller.requests.size());
    }

    @Test
    void invoke_shouldResolveExternalFallbackMethodByName() {
        OldService.COUNT.set(0);
        NewService.FAIL_COUNT.set(0);
        FallbackService.COUNT.set(0);

        FakeConfigClient configClient = new FakeConfigClient();
        configClient.status = MigrationStatus.GO_LIVE_ALL.getCode();
        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
        MigrationInterceptor interceptor = new MigrationInterceptor(
                configClient,
                diffServiceCaller,
                new top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                new MigrationProperties());

        StaticApplicationContext context = new StaticApplicationContext();
        context.registerSingleton("oldServiceBean", OldService.class);
        context.registerSingleton("newServiceBean", NewService.class);
        context.registerSingleton("fallbackServiceBean", FallbackService.class);
        context.refresh();
        interceptor.setApplicationContext(context);

        MigrationAnnotationAdvisor advisor = new MigrationAnnotationAdvisor(interceptor);
        ProxyFactory factory = new ProxyFactory(new CrossBeanFallbackDemoService());
        factory.addAdvisor(advisor);
        CrossBeanFallbackDemoService proxy = (CrossBeanFallbackDemoService) factory.getProxy();

        String result = proxy.query("3001");

        assertEquals("fallback:3001", result);
        assertEquals(1, OldService.COUNT.get());
        assertEquals(1, NewService.FAIL_COUNT.get());
        assertEquals(1, FallbackService.COUNT.get());
        assertEquals(0, diffServiceCaller.requests.size());

        context.close();
    }

    private static final class FakeConfigClient implements ConfigClient {
        private int status = MigrationStatus.OLD.getCode();
        private List<GrayscaleConfig> rules = List.of();

        @Override
        public MigrationConfig getMigrationConfig(String migrationKey) {
            return MigrationConfig.builder().migrationKey(migrationKey).status(status).build();
        }

        @Override
        public List<GrayscaleConfig> getGrayscaleRules(String migrationKey) {
            return rules;
        }
    }

    private static final class FakeDiffServiceCaller implements DiffServiceCaller {
        private final List<DiffRequest> requests = new ArrayList<>();

        @Override
        public void executeDiffAsync(DiffRequest request) {
            requests.add(request);
        }
    }

    public static class DemoService {

        private static final AtomicInteger OLD_COUNT = new AtomicInteger();
        private static final AtomicInteger NEW_COUNT = new AtomicInteger();

        @Migration(key = "demo-api", oldMethod = "queryOld", newMethod = "queryNew")
        public String query(String userId) {
            return "entry:" + userId;
        }

        public String queryOld(String userId) {
            OLD_COUNT.incrementAndGet();
            return "old:" + userId;
        }

        public String queryNew(String userId) {
            NEW_COUNT.incrementAndGet();
            return "new:" + userId;
        }
    }

    public static class CrossBeanDemoService {

        @Migration(
                key = "cross-demo-api",
                oldMethod = "oldServiceBean#queryOld",
                newMethod = "newServiceBean#queryNew")
        public String query(String userId) {
            return "entry:" + userId;
        }
    }

    public static class CrossBeanDotDemoService {

        @Migration(
                key = "cross-dot-demo-api",
                oldMethod = "oldServiceBean.queryOld",
                newMethod = "newServiceBean.queryNew")
        public String query(String userId) {
            return "entry:" + userId;
        }
    }

    public static class CrossBeanFallbackDemoService {

        @Migration(
                key = "cross-fallback-api",
                oldMethod = "oldServiceBean#queryOld",
                newMethod = "newServiceBean#queryFail",
                fallBackMethod = "fallbackServiceBean#queryFallback")
        public String query(String userId) {
            return "entry:" + userId;
        }
    }

    public static class CrossBeanThrowableFallbackDemoService {

        @Migration(
                key = "cross-throwable-fallback-api",
                oldMethod = "oldServiceBean.queryOld",
                newMethod = "newServiceBean.queryFail",
                fallBackMethod = "throwableFallbackServiceBean.queryFallback")
        public String query(String userId) {
            return "entry:" + userId;
        }
    }

    public static class ParamHandlerDemoService {

        private static final AtomicInteger OLD_COUNT = new AtomicInteger();
        private static final AtomicInteger NEW_COUNT = new AtomicInteger();

        @Migration(
                key = "param-handler-demo-api",
                oldMethod = "queryOld",
                newMethod = "queryNew",
                paramHandler = ReflectionParamHandler.class)
        public String query(String userId) {
            return "entry:" + userId;
        }

        public String queryOld(String userId) {
            OLD_COUNT.incrementAndGet();
            return "old:" + userId;
        }

        public String queryNew(String userId) {
            NEW_COUNT.incrementAndGet();
            return "new:" + userId;
        }
    }

    public static class NewFailNoFallbackDemoService {

        private static final AtomicInteger OLD_COUNT = new AtomicInteger();
        private static final AtomicInteger NEW_FAIL_COUNT = new AtomicInteger();

        @Migration(
                key = "new-fail-no-fallback-api",
                oldMethod = "queryOld",
                newMethod = "queryFail")
        public String query(String userId) {
            return "entry:" + userId;
        }

        public String queryOld(String userId) {
            OLD_COUNT.incrementAndGet();
            return "old:" + userId;
        }

        public String queryFail(String userId) {
            NEW_FAIL_COUNT.incrementAndGet();
            throw new IllegalStateException("new fail");
        }
    }

    public static class PrivateFallbackDemoService {

        private static final AtomicInteger OLD_COUNT = new AtomicInteger();
        private static final AtomicInteger NEW_FAIL_COUNT = new AtomicInteger();
        private static final AtomicInteger FALLBACK_COUNT = new AtomicInteger();

        @Migration(
                key = "private-fallback-demo-api",
                oldMethod = "queryOld",
                newMethod = "queryFail",
                fallBackMethod = "queryFallback")
        public String query(String userId) {
            return "entry:" + userId;
        }

        public String queryOld(String userId) {
            OLD_COUNT.incrementAndGet();
            return "old:" + userId;
        }

        public String queryFail(String userId) {
            NEW_FAIL_COUNT.incrementAndGet();
            throw new IllegalStateException("new fail");
        }

        private String queryFallback(String userId, Throwable ex) {
            FALLBACK_COUNT.incrementAndGet();
            return "private-fallback:" + userId;
        }
    }

    public static class ReflectionParamHandler implements ParamHandler {
        @Override
        public Map<String, Object> build(Object... args) {
            return Map.of("userId", args[0]);
        }
    }

    public static class OldService {

        private static final AtomicInteger COUNT = new AtomicInteger();

        public String queryOld(String userId) {
            COUNT.incrementAndGet();
            return "external-old:" + userId;
        }
    }

    public static class NewService {

        private static final AtomicInteger COUNT = new AtomicInteger();
        private static final AtomicInteger FAIL_COUNT = new AtomicInteger();

        public String queryNew(String userId) {
            COUNT.incrementAndGet();
            return "external-new:" + userId;
        }

        public String queryFail(String userId) {
            FAIL_COUNT.incrementAndGet();
            throw new IllegalStateException("new fail");
        }
    }

    public static class FallbackService {

        private static final AtomicInteger COUNT = new AtomicInteger();

        public String queryFallback(String userId, Exception ex) {
            COUNT.incrementAndGet();
            return "fallback:" + userId;
        }
    }

    public static class ThrowableFallbackService {

        private static final AtomicInteger COUNT = new AtomicInteger();

        public String queryFallback(String userId, Throwable ex) {
            COUNT.incrementAndGet();
            return "throwable-fallback:" + userId;
        }
    }
}
