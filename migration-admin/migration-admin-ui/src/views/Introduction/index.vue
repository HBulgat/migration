<template>
  <el-card class="page-card intro-card" shadow="never">
    <h1 class="page-title">后端接口迁移平台</h1>
    <p class="intro-desc">
      这是一个用于后端接口渐进式迁移的系统，帮助团队在保障稳定性的前提下完成旧接口到新接口的平滑演进。
    </p>

    <el-divider />

    <div class="feature-grid">
      <el-card shadow="hover">
        <h3>7 个迁移阶段</h3>
        <p>单旧 → 验证 → 上线 → 停用，支持任意阶段回滚。</p>
      </el-card>
      <el-card shadow="hover">
        <h3>多种灰度策略</h3>
        <p>支持百分比、黑名单、白名单、表达式等规则。</p>
      </el-card>
      <el-card shadow="hover">
        <h3>并行 Diff 比对</h3>
        <p>新旧接口并发调用，对比差异并沉淀记录。</p>
      </el-card>
      <el-card shadow="hover">
        <h3>可视化管理</h3>
        <p>通过 Web 界面管理任务、规则和监控视图。</p>
      </el-card>
    </div>

    <el-divider />

    <h3>快速开始</h3>
    <el-steps direction="vertical" :active="4" finish-status="success">
      <el-step title="创建迁移任务" description="在“迁移任务”页面新建任务并设置初始状态" />
      <el-step title="配置灰度规则" description="在“灰度规则”页面配置分流规则并启用" />
      <el-step title="观察 Diff 大盘" description="在“Diff大盘”页面查看总体差异率和耗时趋势" />
      <el-step title="查看 Diff 详情" description="在“Diff记录”页面定位具体差异字段与响应内容" />
    </el-steps>
  </el-card>
</template>

<style scoped>
.intro-card {
  padding: 8px;
}

.intro-desc {
  color: #606266;
  font-size: 14px;
  margin-top: 8px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

h3 {
  margin: 0 0 8px;
  color: #303133;
}

p {
  margin: 0;
  color: #606266;
}

@media (max-width: 960px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
