//package top.bulgat.migration.sdk.core.client;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import top.bulgat.migration.sdk.core.function.ExecuteFunction;
//import top.bulgat.migration.sdk.core.gray.DefaultGrayMatcher;
//import top.bulgat.migration.sdk.core.model.DiffRequest;
//import top.bulgat.migration.sdk.core.model.GrayConfig;
//import top.bulgat.migration.sdk.core.model.MigrationConfig;
//import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;
//import top.bulgat.migration.sdk.core.spi.ConfigClient;
//import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;
//import top.bulgat.migration.sdk.core.strategy.MigrationStrategyRegistry;
//
//class MigrationClientTest {
//
//    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
//
//    @AfterEach
//    void tearDown() {
//        executorService.shutdown();
//    }
//
//    @Test
//    void wrap_shouldCallOldOnlyWhenStatusOld() {
//        FakeConfigClient configClient = new FakeConfigClient(MigrationTaskStatus.OLD.getCode(), List.of());
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldCount = new AtomicInteger();
//        AtomicInteger newCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldCount.incrementAndGet();
//                    return "old";
//                },
//                args -> {
//                    newCount.incrementAndGet();
//                    return "new";
//                },
//                args -> Map.of("userId", "1001"));
//
//        String result = execute.apply("1001");
//
//        assertEquals("old", result);
//        assertEquals(1, oldCount.get());
//        assertEquals(0, newCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
////    @Test
//    void wrap_shouldReturnOldAndSendDiffWhenValidationGray() {
//        GrayConfig whitelistRule = GrayConfig.builder()
//                .ruleType("WHITELIST")
//                .ruleValue("[\"1001\"]")
//                .enable(true)
//                .build();
//        FakeConfigClient configClient = new FakeConfigClient(
//                MigrationTaskStatus.VALIDATION_GRAY.getCode(),
//                List.of(whitelistRule));
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldCount = new AtomicInteger();
//        AtomicInteger newCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldCount.incrementAndGet();
//                    return "old";
//                },
//                args -> {
//                    newCount.incrementAndGet();
//                    return "new";
//                },
//                args -> Map.of("userId", args[0]));
//
//        String result = execute.apply("1002");
//
//        assertEquals("old", result);
//        assertEquals(1, oldCount.get());
//        assertEquals(1, newCount.get());
//        assertEquals(1, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldReturnNewAndSendDiffWhenGoLiveAll() {
//        FakeConfigClient configClient = new FakeConfigClient(MigrationTaskStatus.GO_LIVE_ALL.getCode(), List.of());
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldCount = new AtomicInteger();
//        AtomicInteger newCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldCount.incrementAndGet();
//                    return "old";
//                },
//                args -> {
//                    newCount.incrementAndGet();
//                    return "new";
//                },
//                args -> Map.of("userId", "1001"));
//
//        String result = execute.apply("1001");
//
//        assertEquals("new", result);
//        assertEquals(1, oldCount.get());
//        assertEquals(1, newCount.get());
//        assertEquals(1, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldReturnNewOnlyWhenGoLiveGrayAndGrayHit() {
//        GrayConfig whitelistRule = GrayConfig.builder()
//                .ruleType("WHITELIST")
//                .ruleValue("[\"1001\"]")
//                .enable(true)
//                .build();
//        FakeConfigClient configClient = new FakeConfigClient(
//                MigrationTaskStatus.GO_LIVE_GRAY.getCode(),
//                List.of(whitelistRule));
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldCount = new AtomicInteger();
//        AtomicInteger newCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldCount.incrementAndGet();
//                    return "old";
//                },
//                args -> {
//                    newCount.incrementAndGet();
//                    return "new";
//                },
//                args -> Map.of("userId", args[0]));
//
//        String result = execute.apply("1001");
//
//        assertEquals("new", result);
//        assertEquals(0, oldCount.get());
//        assertEquals(1, newCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldReturnOldAndSendDiffWhenGoLiveGrayAndGrayMiss() {
//        GrayConfig whitelistRule = GrayConfig.builder()
//                .ruleType("WHITELIST")
//                .ruleValue("[\"1001\"]")
//                .enable(true)
//                .build();
//        FakeConfigClient configClient = new FakeConfigClient(
//                MigrationTaskStatus.GO_LIVE_GRAY.getCode(),
//                List.of(whitelistRule));
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldCount = new AtomicInteger();
//        AtomicInteger newCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldCount.incrementAndGet();
//                    return "old";
//                },
//                args -> {
//                    newCount.incrementAndGet();
//                    return "new";
//                },
//                args -> Map.of("userId", args[0]));
//
//        String result = execute.apply("1002");
//
//        assertEquals("old", result);
//        assertEquals(1, oldCount.get());
//        assertEquals(1, newCount.get());
//        assertEquals(1, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldNotInvokeOldTwiceWhenOldOnlyOldFails() {
//        FakeConfigClient configClient = new FakeConfigClient(MigrationTaskStatus.OLD.getCode(), List.of());
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldFailCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldFailCount.incrementAndGet();
//                    throw new IllegalStateException("old failed");
//                },
//                args -> "new",
//                args -> Map.of("userId", "1001"));
//
//        assertThrows(IllegalStateException.class, () -> execute.apply("1001"));
//        assertEquals(1, oldFailCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldNotInvokeOldTwiceWhenValidationGrayOldFails() {
//        FakeConfigClient configClient = new FakeConfigClient(MigrationTaskStatus.VALIDATION_GRAY.getCode(), List.of());
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldFailCount = new AtomicInteger();
//        AtomicInteger newCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldFailCount.incrementAndGet();
//                    throw new IllegalStateException("old failed");
//                },
//                args -> {
//                    newCount.incrementAndGet();
//                    return "new";
//                },
//                args -> Map.of("userId", "1001"));
//
//        assertThrows(IllegalStateException.class, () -> execute.apply("1001"));
//        assertEquals(1, oldFailCount.get());
//        assertEquals(1, newCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldNotInvokeOldTwiceWhenGoLiveGrayMissOldFails() {
//        GrayConfig whitelistRule = GrayConfig.builder()
//                .ruleType("WHITELIST")
//                .ruleValue("[\"1001\"]")
//                .enable(true)
//                .build();
//        FakeConfigClient configClient = new FakeConfigClient(
//                MigrationTaskStatus.GO_LIVE_GRAY.getCode(),
//                List.of(whitelistRule));
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldFailCount = new AtomicInteger();
//        AtomicInteger newCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldFailCount.incrementAndGet();
//                    throw new IllegalStateException("old failed");
//                },
//                args -> {
//                    newCount.incrementAndGet();
//                    return "new";
//                },
//                args -> Map.of("userId", args[0]));
//
//        assertThrows(IllegalStateException.class, () -> execute.apply("1002"));
//        assertEquals(1, oldFailCount.get());
//        assertEquals(1, newCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldNotInvokeOldTwiceWhenDecommissioningGrayMissAndNewFails() {
//        GrayConfig whitelistRule = GrayConfig.builder()
//                .ruleType("WHITELIST")
//                .ruleValue("[\"1001\"]")
//                .enable(true)
//                .build();
//        FakeConfigClient configClient = new FakeConfigClient(
//                MigrationTaskStatus.DECOMMISSIONING_GRAY.getCode(),
//                List.of(whitelistRule));
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldCount = new AtomicInteger();
//        AtomicInteger newFailCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldCount.incrementAndGet();
//                    return "old";
//                },
//                args -> {
//                    newFailCount.incrementAndGet();
//                    throw new IllegalStateException("new failed");
//                },
//                args -> Map.of("userId", args[0]));
//
//        String result = execute.apply("1002");
//
//        assertEquals("old", result);
//        assertEquals(1, oldCount.get());
//        assertEquals(1, newFailCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
////    @Test
//    void wrap_shouldNotInvokeOldTwiceWhenGoLiveAllBothBranchesFail() {
//        FakeConfigClient configClient = new FakeConfigClient(MigrationTaskStatus.GO_LIVE_ALL.getCode(), List.of());
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldFailCount = new AtomicInteger();
//        AtomicInteger newFailCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldFailCount.incrementAndGet();
//                    throw new IllegalStateException("old failed");
//                },
//                args -> {
//                    newFailCount.incrementAndGet();
//                    throw new IllegalStateException("new failed");
//                },
//                args -> Map.of("userId", "1001"));
//
//        assertThrows(IllegalStateException.class, () -> execute.apply("1001"));
//        assertEquals(1, oldFailCount.get());
//        assertEquals(1, newFailCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldNotInvokeOldTwiceWhenDecommissioningGrayMissBothFail() {
//        GrayConfig whitelistRule = GrayConfig.builder()
//                .ruleType("WHITELIST")
//                .ruleValue("[\"1001\"]")
//                .enable(true)
//                .build();
//        FakeConfigClient configClient = new FakeConfigClient(
//                MigrationTaskStatus.DECOMMISSIONING_GRAY.getCode(),
//                List.of(whitelistRule));
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldFailCount = new AtomicInteger();
//        AtomicInteger newFailCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldFailCount.incrementAndGet();
//                    throw new IllegalStateException("old failed");
//                },
//                args -> {
//                    newFailCount.incrementAndGet();
//                    throw new IllegalStateException("new failed");
//                },
//                args -> Map.of("userId", args[0]));
//
//        assertThrows(IllegalStateException.class, () -> execute.apply("1002"));
//        assertEquals(1, oldFailCount.get());
//        assertEquals(1, newFailCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
//    @Test
//    void wrap_shouldOnlyCallNewWhenDecommissioningGrayAndGrayHit() {
//        GrayConfig whitelistRule = GrayConfig.builder()
//                .ruleType("WHITELIST")
//                .ruleValue("[\"1001\"]")
//                .enable(true)
//                .build();
//        FakeConfigClient configClient = new FakeConfigClient(
//                MigrationTaskStatus.DECOMMISSIONING_GRAY.getCode(),
//                List.of(whitelistRule));
//        FakeDiffServiceCaller diffServiceCaller = new FakeDiffServiceCaller();
//        MigrationClient client = buildClient(configClient, diffServiceCaller);
//
//        AtomicInteger oldCount = new AtomicInteger();
//        AtomicInteger newCount = new AtomicInteger();
//        ExecuteFunction<String> execute = client.wrap(
//                args -> {
//                    oldCount.incrementAndGet();
//                    return "old";
//                },
//                args -> {
//                    newCount.incrementAndGet();
//                    return "new";
//                },
//                args -> Map.of("userId", args[0]));
//
//        String result = execute.apply("1001");
//
//        assertEquals("new", result);
//        assertEquals(0, oldCount.get());
//        assertEquals(1, newCount.get());
//        assertEquals(0, diffServiceCaller.requests.size());
//    }
//
//    private MigrationClient buildClient(FakeConfigClient configClient, FakeDiffServiceCaller diffServiceCaller) {
//        return new MigrationClient(
//                MigrationConfig.builder().migrationKey("user-getUser-api").build(),
//                configClient,
//                diffServiceCaller,
//                new DefaultGrayMatcher(),
//                MigrationStrategyRegistry.defaultRegistry(),
//                executorService);
//    }
//
//    private static final class FakeConfigClient implements ConfigClient {
//        private final int status;
//        private final List<GrayConfig> rules;
//
//        private FakeConfigClient(int status, List<GrayConfig> rules) {
//            this.status = status;
//            this.rules = rules;
//        }
//
//        @Override
//        public MigrationConfig getMigrationConfig(String migrationKey) {
//            return MigrationConfig.builder()
//                    .migrationKey(migrationKey)
//                    .status(status)
//                    .build();
//        }
//
//        @Override
//        public List<GrayConfig> getGrayRules(String migrationKey) {
//            return rules;
//        }
//    }
//
//    private static final class FakeDiffServiceCaller implements DiffServiceCaller {
//        private final List<DiffRequest> requests = new ArrayList<>();
//
//        @Override
//        public void executeDiffAsync(DiffRequest request) {
//            requests.add(request);
//        }
//    }
//}
