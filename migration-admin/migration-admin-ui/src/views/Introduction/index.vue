<template>
  <el-card class="page-card intro-card" shadow="never">
    <div class="intro-header">
      <h1 class="page-title">后端接口迁移平台</h1>
      <el-tag type="success" effect="light" class="version-tag">V1.0</el-tag>
    </div>
    
    <p class="intro-desc">
      这是一个用于解决微服务架构或遗留系统升级场景中，业务底层接口从旧版本迁移到新版本时面临高风险问题的综合性解决方案。通过渐进式迁移、灰度验证、并行对比（Diff）等机制，确保接口迁移过程的安全、平滑与可控，最大程度降低线上事故风险。
    </p>

    <el-divider />

    <div class="feature-grid">
      <el-card shadow="hover" class="feature-item">
        <template #header>
          <div class="card-header">
            <el-icon class="feature-icon"><Connection /></el-icon>
            <span class="feature-title">平滑渐进式迁移</span>
          </div>
        </template>
        <p class="feature-desc">将迁移过程标准化为7个渐进阶段：单旧 → 验证灰度 → 验证全开 → 上线灰度 → 上线全开 → 停用灰度 → 停用全开。支持精准降本控险，向后可随时一键回滚。</p>
      </el-card>

      <el-card shadow="hover" class="feature-item">
        <template #header>
          <div class="card-header">
            <el-icon class="feature-icon"><Filter /></el-icon>
            <span class="feature-title">多维度灰度策略</span>
          </div>
        </template>
        <p class="feature-desc">支持灵活多样的分流规则，包含按百分比（Percentage）、白名单（Whitelist）、黑名单（Blacklist）机制，以及基于 SpEL 的表达式（Expression）自定义匹配进行流量路由。</p>
      </el-card>

      <el-card shadow="hover" class="feature-item">
        <template #header>
          <div class="card-header">
            <el-icon class="feature-icon"><Compass /></el-icon>
            <span class="feature-title">并行比对验证 (Diff)</span>
          </div>
        </template>
        <p class="feature-desc">在不影响线上响应的情况下，异步并发调用新旧接口。通过独立支持横向扩展的 Diff 服务比对结果数据。不仅可以忽略字段对比，也支持数值容差、SpEL 自定义判齐及按特殊字段排序后再对比等高级功能。</p>
      </el-card>

      <el-card shadow="hover" class="feature-item">
        <template #header>
          <div class="card-header">
            <el-icon class="feature-icon"><Monitor /></el-icon>
            <span class="feature-title">控制台可视化管理</span>
          </div>
        </template>
        <p class="feature-desc">通过 Nacos/Apollo 实时分发全网配置，借助 Web 前端应用管理迁移任务，实时直观观察大盘统计数据、接口耗时分布、错误率图表及精细入微的 JSON-Patch 等级的差异分析记录。</p>
      </el-card>
    </div>

    <el-divider />

    <div class="guide-section">
      <h3>🚀 快速开始指引</h3>
      <el-steps :active="activeStep" finish-status="success" simple class="guide-steps">
        <el-step title="客户端接入 SDK" icon="Cpu" />
        <el-step title="创建业务迁移任务" icon="DocumentAdd" />
        <el-step title="配置对比与灰度规则" icon="Operation" />
        <el-step title="调控阶段并观察监控" icon="DataAnalysis" />
      </el-steps>
      
      <div class="guide-content">
        <p>如需使用，请从您的 Spring Boot 应用项目引入 <code>migration-spring-boot-starter</code>，利用 <code>@Migration</code> 注解以毫无侵入式的方式，将旧接口与全新的重构逻辑进行关联。之后在此控制台管理阶段即可。</p>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const activeStep = ref(4)
</script>

<style scoped>
.intro-card {
  padding: 16px;
  border-radius: 8px;
  background-color: #ffffff;
}

.intro-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #303133;
  font-weight: 600;
}

.version-tag {
  font-size: 14px;
}

.intro-desc {
  color: #606266;
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin: 24px 0;
}

.feature-item {
  border-radius: 8px;
  transition: all 0.3s;
}

.feature-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.feature-icon {
  font-size: 20px;
  color: #409EFF;
}

.feature-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.feature-desc {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.guide-section {
  margin-top: 24px;
}

.guide-section h3 {
  margin: 0 0 20px;
  color: #303133;
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.guide-steps {
  margin-bottom: 24px;
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 12px 24px;
}

.guide-content p {
  color: #606266;
  font-size: 15px;
  line-height: 1.6;
  background-color: #ecf5ff;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
  margin: 0;
}

@media (max-width: 960px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
