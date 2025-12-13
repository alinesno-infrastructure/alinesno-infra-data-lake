<template>
  <div>
    <el-row class="acp-dashboard-panel" :gutter="20">

       <el-col class="panel-col" :span="12">
        <div class="grid-content">
          <div class="panel-header">
            <div class="header-title"><i class="fa-solid fa-computer"></i> 最近新增资产</div>
          </div>
          <div class="panel-body" style="height: auto;position: relative;">
            <!-- 列出最近新增加数据资产 -->
            <div class="assets-list">
              <div 
                class="asset-item" 
                v-for="asset in recentAssets" 
                :key="asset.id"
                @mouseenter="asset.hover = true"
                @mouseleave="asset.hover = false"
              >
                <div class="asset-icon">
                  <i :class="getAssetIcon(asset.type)" :alt="asset.typeText"></i>
                </div>
                <div class="asset-info">
                  <div class="asset-name">
                    {{ asset.name }}
                      <el-button size="small" type="text">
                        {{ asset.typeText }}
                      </el-button> 

                  </div>
                  <div class="asset-desc line-clamp-2">
                      {{ formatTime(asset.createTime) }}
                    {{ asset.description }}</div>
                </div>

                <!-- <div class="asset-actions" :class="{ 'visible': asset.hover }">
                  <el-button size="mini" type="text" icon="fa-solid fa-eye" @click="viewAsset(asset)">查看</el-button>
                  <el-button size="mini" type="text" icon="fa-solid fa-ellipsis-v" @click="moreActions(asset)"></el-button>
                </div> -->

              </div>
              
              <!-- 空状态处理 -->
              <!-- <div class="empty-state" v-if="recentAssets.length === 0">
                <i class="fa-solid fa-folder-open"></i>
                <p>暂无新增资产</p>
                <el-button type="primary" size="small" @click="addNewAsset">
                  <i class="fa-solid fa-plus"></i> 新增资产
                </el-button>
              </div> -->

            </div>
          </div>
        </div>
      </el-col>

      <el-col class="panel-col" :span="12">
        <div class="grid-content">
          <div class="panel-header">
            <div class="header-title"><i class="fa-solid fa-link"></i> 最近业务目录 </div>
          </div>
          <div class="panel-body" style="height: auto;position: relative;padding-right: 0px;">
              <div class="direct-box">
                  <div class="box-item catalog-item" v-for="item in dataLakeAdvantages" :key="item">
                    <div class="dire-panel">
                      <div class="panel-title">
                        <i class="dire-panel-icon" :class="item.icon" :alt="item.name" />
                        {{ item.name }}
                      </div>
                      <div class="panel-content">
                        <p class="catalog-description">
                          <el-text line-clamp="2" v-if="item.desc">
                            {{ item.desc}}
                          </el-text>
                          <el-text line-clamp="2" v-else>
                            暂无描述
                          </el-text>
                        </p>
                      </div> 
                      <div class="panel-describe" >
                        <div class="metric">
                          <div class="metric-label">
                            <i class="fa-regular fa-file-lines"></i> <!-- 替换原 fa-file-text-o -->
                            <span>数据表数</span>
                          </div>
                          <span class="metric-value">{{ item.tableCount || '0 GB' }}</span>
                        </div>

                        <div class="metric">
                          <div class="metric-label">
                            <i class="fa-solid fa-database"></i> <!-- 6.x 中数据库图标类未变，但需确认使用 solid 风格 -->
                            <span>元数据量</span>
                          </div>
                          <span class="metric-value">{{ item.storageSize || '0 GB'  }}</span>
                        </div>

                      </div>
                      <!-- <div class="panel-tip">访问链接</div> -->
                    </div>
                  </div>
                  <div v-if="dataLakeAdvantages.length === 0">
                    <el-empty description="暂无数据" ></el-empty>
                  </div>
              </div>
          </div>
        </div>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { ref } from 'vue';

import {
  latestCatalogs,
} from '@/api/data/lake/catalog';

// 最近新增资产数据
const recentAssets = ref([
  {
    id: 1,
    name: '用户行为分析数据集',
    type: 'dataset',
    typeText: '数据集',
    description: '包含2023年第三季度用户点击、浏览、购买等行为数据，用于用户画像分析',
    createTime: new Date(Date.now() - 86400000 * 1), // 1天前
    hover: false
  },
  {
    id: 2,
    name: '产品销售预测模型',
    type: 'model',
    typeText: '模型',
    description: '基于LSTM的产品销量预测模型，预测准确率达89.7%',
    createTime: new Date(Date.now() - 86400000 * 2), // 2天前
    hover: false
  },
  {
    id: 3,
    name: '客户满意度调查结果',
    type: 'report',
    typeText: '报告',
    description: '2023年Q3客户满意度调查分析报告，包含NPS评分及改进建议',
    createTime: new Date(Date.now() - 86400000 * 3), // 3天前
    hover: false
  },
  {
    id: 4,
    name: '区域销售分布看板',
    type: 'dashboard',
    typeText: '仪表盘',
    description: '全国各区域销售数据实时监控看板，支持钻取分析',
    createTime: new Date(Date.now() - 86400000 * 5), // 5天前
    hover: false
  }
]);

// 数据湖产品优势（左侧主区域数据）
const dataLakeAdvantages = ref([]);

// 获取资产对应的图标
const getAssetIcon = (type) => {
  const icons = {
    dataset: 'fa-solid fa-database',
    model: 'fa-solid fa-brain',
    report: 'fa-solid fa-file-pdf',
    dashboard: 'fa-solid fa-chart-line'
  };
  return icons[type] || 'fa-solid fa-file';
};

// 格式化时间显示 - 使用原生JavaScript实现
const formatTime = (time) => {
  const now = new Date();
  const diffInSeconds = Math.floor((now - time) / 1000);
  
  if (diffInSeconds < 60) {
    return `${diffInSeconds}秒前`;
  } else if (diffInSeconds < 3600) {
    return `${Math.floor(diffInSeconds / 60)}分钟前`;
  } else if (diffInSeconds < 86400) {
    return `${Math.floor(diffInSeconds / 3600)}小时前`;
  } else if (diffInSeconds < 604800) {
    return `${Math.floor(diffInSeconds / 86400)}天前`;
  } else {
    // 原生JS日期格式化
    const year = time.getFullYear();
    const month = String(time.getMonth() + 1).padStart(2, '0');
    const day = String(time.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
};

nextTick(() => {
  // 获取最近资产数据
  latestCatalogs().then((res) => {
    dataLakeAdvantages.value = res.data.map((item) => {
      return {
        id: item.id,
        icon: getAssetIcon(item.type),
        name: item.catalogName,
        desc: item.description,
        createTime: formatTime(new Date(item.createTime))
      }
    });
  })
})

</script>

<style lang="scss" scoped>
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 0px solid #eee;
}

.header-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
}

.header-title i {
  margin-right: 8px;
  color: #409eff;
}

.more-btn {
  color: #409eff;
  padding: 0;
  height: auto;
}

.assets-list {
  padding: 0px 0;
}

.asset-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s;
}

.asset-item:last-child {
  border-bottom: none;
}

.asset-item:hover {
  background-color: #fafafa;
}

.asset-icon {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background-color: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.asset-icon i {
  font-size: 20px;
  color: #409eff;
}

.asset-info {
  flex-grow: 1;
  min-width: 0;
}

.asset-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.asset-meta {
  display: flex;
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.asset-type {
  margin-right: 16px;
  background-color: #f0f9eb;
  color: #52c41a;
  padding: 2px 6px;
  border-radius: 4px;
}

.metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
}

.metric-value {
  font-weight: 500;
  font-size: 13px;
  color: #1f2937;
}

.asset-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.asset-actions {
  display: flex;
  opacity: 0;
  transition: opacity 0.2s;
}

.asset-actions.visible {
  opacity: 1;
}

.asset-actions .el-button {
  padding: 0 6px;
  margin-left: 8px;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
  color: #999;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  color: #ccc;
}

.empty-state p {
  margin-bottom: 20px;
}

.catalog-item {
  width: calc(33% - 10px);
  margin-right: 10px;
  background-color: #f5f5f5;
  border-radius: 8px;
  border-left: 0px !important;
  margin-top: 10px;
}

.catalog-description{
  padding: 0px;
  margin: 0px;
  line-height: 1rem;
  height:35px;
}

</style>