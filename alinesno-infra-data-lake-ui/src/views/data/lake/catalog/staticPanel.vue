<template>
  <div class="stats-grid">
    <!-- 使用v-for循环渲染渲染统计卡片 -->
    <div class="stat-card" v-for="(item, index) in statsItems" :key="index">
      <div class="stat-content">
        <div class="stat-info">
          <p class="stat-label">{{ item.label }}</p>
          <h3 class="stat-value">
            {{ item.value !== null ? (item.format ? item.format(item.value) : item.value) : '加载中...' }}
          </h3>
          <p class="stat-trend" :class="item.trendClass">
            <i :class="item.trendIcon"></i> {{ item.trendText }}
          </p>
        </div>
        <div class="stat-icon" :class="item.iconClass">
          <i :class="item.icon"></i>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { countStats } from '@/api/data/lake/catalog'
import { onMounted, ref, computed } from 'vue';

// 原始统计数据
const rawStats = ref({
  totalStorageSize: null,
  schemeCount: null,
  schemeTableCount: null
})

// 格式化数字为带千分位的格式
const formatNumber = (num) => {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// 统计项配置 - 定义每个卡片的显示内容和样式
const statsItems = computed(() => [
  {
    label: '总存储量',
    value: rawStats.value.totalStorageSize,
    trendClass: 'trend-up',
    trendIcon: 'fa fa-arrow-up',
    trendText: '较上月增长 23%',
    iconClass: 'icon-accent',
    icon: 'fa-solid fa-hard-drive',
    format: null  // 不需要格式化
  },
  {
    label: '总领域数',
    value: rawStats.value.schemeCount,
    trendClass: 'trend-up',
    trendIcon: 'fa fa-arrow-up',
    trendText: '较上月增长 12%',
    iconClass: 'icon-primary',
    icon: 'fa fa-folder-open',
    format: null  // 不需要格式化
  },
  {
    label: '总数据表',
    value: rawStats.value.schemeTableCount,
    trendClass: 'trend-up',
    trendIcon: 'fa fa-arrow-up',
    trendText: '较上月增长 8%',
    iconClass: 'icon-secondary',
    icon: 'fa fa-table',
    format: formatNumber  // 需要千分位格式化
  }
])

// 获取统计数据
const handleCountStats = async () => {
  try {
    const res = await countStats()
    if (res.code === 200) {
      // 更新原始统计数据
      rawStats.value = {
        totalStorageSize: res.data.totalStorageSize,
        schemeCount: res.data.schemeCount,
        schemeTableCount: res.data.schemeTableCount
      }
    } else {
      console.error('获取统计数据失败:', res.message)
    }
  } catch (error) {
    console.error('获取统计数据时发生错误:', error)
  }
}

// 组件挂载时获取数据
onMounted(() => {
  handleCountStats()
})
</script>

<style lang="scss" scoped>
/* 统计卡片样式保持不变 */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

@media (min-width: 640px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.stat-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.stat-card {
  background-color: white;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid rgba(6, 7, 9, .1);
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 0px;
  margin-top: 0px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-top: 8px;
  margin-bottom: 8px;
  color: #1f2937;
}

.stat-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 0px;
  margin-top: 0px;
}

.trend-up {
  color: #1d75b0;
}

.trend-down {
  color: #FF4D4F;
}

// 为不同图标添加颜色
.icon-accent {
  background-color: rgba(29, 117, 176, 0.1);
  color: #1d75b0;
}

.icon-primary {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.icon-secondary {
  background-color: rgba(16, 185, 129, 0.1);
  color: #10b981;
}
</style>
    