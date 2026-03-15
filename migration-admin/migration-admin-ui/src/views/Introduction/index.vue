<template>
  <div class="intro-container">
    <div class="header-section">
      <div class="header-content">
        <h1 class="title">
          <img :src="logoImg" class="title-logo" />
          后端接口迁移平台
          <el-tag type="success" effect="dark" round class="version-badge">v1.0.0</el-tag>
        </h1>
        <p class="feature-desc">通过 Nacos 实时分发全网配置，借助 Web 前端应用管理迁移任务，实时直观观察大盘统计数据、接口耗时分布、错误率图表及精细入微的 JSON-Patch 等级的差异分析记录。</p>
        <p class="desc">
          在业务底层接口从旧版本迁移到新版本的场景中，直接切换往往伴随着极大风险。本平台通过<strong>渐进式迁移</strong>、<strong>多维灰度验证</strong>、<strong>实时并行比对（Diff）</strong>等核心机制，提供了一套开箱即用的闭环解决方案。它不仅能确保迁移过程的安全可控，更能最大程度降低因新接口Bug或不兼容导致的线上事故。
        </p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="content-tabs">
      <!-- 平台优势与核心价值 -->
      <el-tab-pane label="💎 核心价值与优势" name="value">
        <div class="tab-content">
          <el-row :gutter="24">
            <el-col :span="8">
              <el-card shadow="hover" class="value-card">
                <template #header>
                  <div class="card-title">
                    <el-icon class="icon success"><CircleCheckFilled /></el-icon>
                    <span>零风险平滑演进</span>
                  </div>
                </template>
                <p>将高危的“一次性切换”拆解为7个标准化渐进阶段。在验证阶段，所有线上调用的真实请求均锚定“旧接口”响应，新接口的调用仅用于离线旁路验证。即便新接口出现严重故障，也对线上业务<strong>0影响</strong>。</p>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" class="value-card">
                <template #header>
                  <div class="card-title">
                    <el-icon class="icon warning"><HelpFilled /></el-icon>
                    <span>强大的 Diff 对比引擎</span>
                  </div>
                </template>
                <p>内置高性能独立 Diff 服务。支持不仅限于完全一致的比对，更支持<strong>忽略特定字段</strong>、<strong>数值容差处理</strong>、<strong>SpEL复杂脚本判齐</strong>以及<strong>无序数组重排序规则</strong>，满足各种复杂业务场景的兼容性验证诉求。</p>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" class="value-card">
                <template #header>
                  <div class="card-title">
                    <el-icon class="icon primary"><Connection /></el-icon>
                    <span>极简无侵入接入</span>
                  </div>
                </template>
                <p>对 Spring Boot 提供深度集成的 Starter。开发者只需使用 <code>@Migration</code> 注解标注 Controller/Service 方法，平台底层的 AOP 切面与 Disruptor 高性能异步队列即可自动接管流量分发与数据上报，业务代码0污染。</p>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 7大核心迁移阶段 -->
      <el-tab-pane label="🔄 七步迁移法" name="stages">
        <div class="tab-content">
          <div class="timeline-wrapper">
            <el-timeline>
              <el-timeline-item center timestamp="阶段 1" placement="top" type="info">
                <el-card shadow="never">
                  <h4>单旧跑 (Old Only)</h4>
                  <p>系统初始化状态。所有流量100%路由至旧接口，新接口完全不参与调用，确保系统保持原有稳定性基线。</p>
                </el-card>
              </el-timeline-item>
              
              <el-timeline-item center timestamp="阶段 2" placement="top" type="primary">
                <el-card shadow="never">
                  <h4>验证-灰度 (Validation-Gray)</h4>
                  <p>配置灰度规则（如白名单员工）。命中的请求会并发调用新旧接口。<strong>此阶段所有请求依然严格返回旧接口结果</strong>，但新旧接口的响应将异步发送到 Diff 引擎做一致性校验分析。</p>
                </el-card>
              </el-timeline-item>

              <el-timeline-item center timestamp="阶段 3" placement="top" type="primary">
                <el-card shadow="never">
                  <h4>验证-全量 (Validation-All)</h4>
                  <p>去除灰度限制，全量线上真实流量并发双写双读。验证系统在新版全量流量下的性能表现及边缘数据的处理正确性。（同样只返回旧接口响应）。</p>
                </el-card>
              </el-timeline-item>
              <el-timeline-item center timestamp="阶段 4" placement="top" type="warning" size="large">
                <el-card shadow="never" class="highlight-card">
                  <h4><el-icon><WarnTriangleFilled /></el-icon> 上线-灰度 (Go-Live-Gray)</h4>
                  <p><strong>关键变点：首次将新接口结果返回给调用方。</strong>命中的请求只调新接口并返回；未命中的请求并发调用新旧接口，不仅做Diff比对，并确保返回老接口结果。此阶段开始逐步切量真实业务。</p>
                </el-card>
              </el-timeline-item>

              <el-timeline-item center timestamp="阶段 5" placement="top" type="success">
                <el-card shadow="never">
                  <h4>上线-全量 (Go-Live-All)</h4>
                  <p>正式上线。所有流量并发调用新旧接口，<strong>统一返回新接口结果</strong>，并将两边响应发送至 Diff 引擎做“上线后维稳对比”，确保全面接管后的数据依然完全一致。</p>
                </el-card>
              </el-timeline-item>

              <el-timeline-item center timestamp="阶段 6" placement="top" type="warning">
                <el-card shadow="never">
                  <h4>停用-灰度 (Decommissioning-Gray)</h4>
                  <p>进入老系统下线倒计时。命中的请求完全脱离老系统（单跑新接口，不Diff），未命中的请求继续并发Diff并返回新接口响应。</p>
                </el-card>
              </el-timeline-item>

              <el-timeline-item center timestamp="阶段 7" placement="top" type="danger">
                <el-card shadow="never">
                  <h4>停用-全量 (Decommissioning-All)</h4>
                  <p>迁移胜利闭环。全面停止对老接口的任何调用及 Diff 逻辑，只单跑新接口。老代码与路由即可安全移除。</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </el-tab-pane>

      <!-- 灰度与流量控制 -->
      <el-tab-pane label="🚦 动态流量控制" name="routing">
        <div class="tab-content">
          <div class="desc-box">
            本平台依赖 Nacos 动态配置中心进行秒级的规则变更下发，涵盖以下精确的分流策略：
          </div>
          <el-descriptions border :column="1" direction="vertical" class="margin-top">
            <el-descriptions-item>
              <template #label><div class="desc-label"><el-icon><PieChart /></el-icon> 百分比 (Percentage)</div></template>
              随机或一致性哈希，指定一定比例（例如 5% 或 30%）的流量进入对应状态，常用于大规模无差别验证。
            </el-descriptions-item>
            <el-descriptions-item>
              <template #label><div class="desc-label"><el-icon><Stamp /></el-icon> 白名单 (Whitelist)</div></template>
              指定特定标识（如开发人员的 UserID、测试专用的 TenantID），极为适合早期内部验证，将风险隔离在小圈子内。
            </el-descriptions-item>
            <el-descriptions-item>
              <template #label><div class="desc-label"><el-icon><Lock /></el-icon> 黑名单 (Blacklist)</div></template>
              在放量时，将极其重要或敏感的客户/租户挡在门外，确保其业务采用旧版本绝对稳妥。
            </el-descriptions-item>
            <el-descriptions-item>
              <template #label><div class="desc-label"><el-icon><Cpu /></el-icon> 表达式 (Expression)</div></template>
              最灵活的兜底方案！支持直接使用 SpEL 脚本编写复杂的匹配逻辑，例如 <code>#amount > 10000 && #city == 'BJ'</code>，精细把控每一次流转。
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-tab-pane>

      <!-- 快速接入指引 -->
      <el-tab-pane label="💻 开发集成指南" name="guide">
        <div class="tab-content">
          <div class="guide-box">
            <h3>Spring Boot 系统接入姿势</h3>
            
            <div class="step">
              <div class="step-num">1</div>
              <div class="step-body">
                <strong>引入依赖并全局开启</strong>
                <p>在项目中引入 <code>migration-spring-boot-starter</code> 的 Maven 坐标，并在启动类增加 <code>@EnableMigration</code> 注解开启扫描：</p>
                <vue-monaco-editor
                  v-model:value="codeSpringBoot"
                  theme="vs-light"
                  language="xml"
                  :options="editorOptions"
                  class="monaco-container"
                  style="height: 105px;"
                />
              </div>
            </div>

            <div class="step">
              <div class="step-num">2</div>
              <div class="step-body">
                <strong>构建参数提取器 (ParamHandler)</strong>
                <p>由于注解拦截在不同方法的参数结构各异，实现该接口可以告诉平台该取哪些参数做“灰度路由匹配”：</p>
<vue-monaco-editor
                  v-model:value="codeParamHandler"
                  theme="vs-light"
                  language="java"
                  :options="editorOptions"
                  class="monaco-container"
                  style="height: 148px;"
                />
              </div>
            </div>

            <div class="step">
              <div class="step-num">3</div>
              <div class="step-body">
                <strong>标注业务方法使用 <code>@Migration</code></strong>
                <p>只需一个注解完成流量劫持。原先的方法体留空，将请求转交给实际的旧逻辑与新逻辑函数。</p>
<vue-monaco-editor
                  v-model:value="codeMigration"
                  theme="vs-light"
                  language="java"
                  :options="editorOptions"
                  class="monaco-container"
                  style="height: 170px;"
                />
              </div>
            </div>

            <div class="step">
              <div class="step-num">4</div>
              <div class="step-body">
                <strong>享受控制台指点江山</strong>
                <p>此时应用会
                  从配置中心拉取 <code>user-login-api</code> 任务的状态。你现在可以通过左侧菜单，新建该任务，调整它在1至7的阶段流转，按需配置灰度和Diff规则。大盘与明细尽收眼底。</p>
              </div>
            </div>
          </div>

          <el-divider border-style="dashed" />

          <div class="guide-box" style="margin-top: 40px;">
            <h3>Golang 系统接入姿势</h3>
            
            <div class="step">
              <div class="step-num">1</div>
              <div class="step-body">
                <strong>引入 Go Modules 依赖</strong>
                <p>在你的 Go 项目中获取 SDK 包：</p>
                <vue-monaco-editor
                  v-model:value="codeGoClient"
                  theme="vs-light"
                  language="shell"
                  :options="editorOptions"
                  class="monaco-container"
                  style="height: 22px;"
                />
              </div>
            </div>

            <div class="step">
              <div class="step-num">2</div>
              <div class="step-body">
                <strong>初始化并使用 Wrapper 封装接管</strong>
                <p>配置 Nacos 地址并初始化客户端，随后即可使用 <code>migration.Wrap</code> 封装你的新旧函数及提取规则。</p>
<vue-monaco-editor
                  v-model:value="codeGoWrap"
                  theme="vs-light"
                  language="go"
                  :options="editorOptions"
                  class="monaco-container"
                  style="height: 170px;"
                />
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import VueMonacoEditor from '@guolao/vue-monaco-editor'
import logoImg from '@/assets/logo.png'

const codeSpringBoot = ref(`<dependency>
    <groupId>top.bulgat.migration</groupId>
    <artifactId>migration-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>`)

const codeParamHandler = ref(`@Component
public class UserParamHandler implements ParamHandler {
    @Override
    public Map<String, Object> build(Object... args) {
        return Map.of("userId", args[0]); 
    }
}`)

const codeMigration = ref(`@Migration(
    key = "user-login-api",          // 在控制台填写的任务ID
    oldMethod = "doLoginOld",        // 遗留逻辑
    newMethod = "doLoginNew",        // 重构的新逻辑
    paramHandler = UserParamHandler.class
)
@PostMapping("/login")
public Result login(@RequestBody LoginReq req) { return null; }`)

const codeGoClient = ref(`go get github.com/HBulgat/migration-sdk-go`)

const codeGoWrap = ref(`config := &migration.Config{
  AdminUrl:       "https://migration.bulgat.top",
  DiffServiceUrl: "https://diff-migration.bulgat.top",
}
client := migration.NewClient(config)
executeFn := client.Wrap("user-getUser-api", targetOld, targetNew, targetFallback, userParamHandler)
res, err := executeFn.Execute("1001", 5)`)

const editorOptions: any = {
  readOnly: true,
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  wordWrap: 'on',
  fontSize: 14,
  fontFamily: "'Fira Code', Consolas, Monaco, monospace",
  lineNumbers: 'off',
  renderLineHighlight: 'none',
  scrollbar: {
    vertical: 'hidden',
    horizontal: 'hidden'
  }
}

import {
  CircleCheckFilled,
  HelpFilled,
  Connection,
  WarnTriangleFilled,
  PieChart,
  Stamp,
  Lock,
  Cpu
} from '@element-plus/icons-vue'

const activeTab = ref('value')
</script>

<style scoped>
.intro-container {
  background: transparent;
  min-height: calc(100vh - 120px);
}

.header-section {
  background: white;
  padding: 40px 48px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  margin-bottom: 24px;
}

.header-content {
  max-width: 900px;
}

.title {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 20px;
}

.title-logo {
  width: 60px;
  height: 60px;
  object-fit: contain;
}

.version-badge {
  font-family: monospace;
  transform: translateY(-4px);
  margin-left: 8px;
}

.subtitle {
  font-size: 18px;
  color: #409eff;
  margin: 12px 0 24px 0;
  font-weight: 500;
}

.desc {
  font-size: 15px;
  line-height: 1.8;
  color: #5e6d82;
  margin: 0;
  text-align: justify;
}

.content-tabs {
  background: white;
  border-radius: 12px;
  padding: 8px 32px 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

:deep(.el-tabs__item) {
  font-size: 16px;
  height: 56px;
  line-height: 56px;
}

.tab-content {
  padding-top: 24px;
}

/* 价值亮点卡片 */
.value-card {
  height: 100%;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.value-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.06);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.card-title .icon {
  font-size: 24px;
  padding: 8px;
  border-radius: 8px;
}

.card-title .icon.success { background: #f0f9eb; color: #67c23a; }
.card-title .icon.warning { background: #fdf6ec; color: #e6a23c; }
.card-title .icon.primary { background: #ecf5ff; color: #409eff; }

.value-card p {
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
  margin: 0;
}

/* 时间轴 */
.timeline-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

.timeline-wrapper h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.timeline-wrapper p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
}

.highlight-card {
  border: 1px solid #faecd8;
  background-color: #fdf6ec;
}

/* 描述列表 */
.desc-box {
  background-color: #f4f4f5;
  padding: 16px 20px;
  border-radius: 6px;
  color: #606266;
  margin-bottom: 24px;
  font-size: 15px;
}

.desc-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  color: #303133;
}

/* 集成指南 */
.guide-box {
  max-width: 800px;
}

.guide-box h3 {
  margin: 0 0 32px 0;
  color: #303133;
  font-size: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.step {
  display: flex;
  margin-bottom: 40px;
}

.step-num {
  width: 36px;
  height: 36px;
  background: #409eff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  flex-shrink: 0;
  margin-right: 20px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.step-body {
  flex: 1;
  min-width: 0;
}

.step-body strong {
  display: block;
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
  line-height: 36px; /* align with number */
}

.monaco-container {
  border-radius: 3px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.step-body p {
  color: #606266;
  line-height: 1.6;
  margin: 0 0 16px 0;
}
</style>
