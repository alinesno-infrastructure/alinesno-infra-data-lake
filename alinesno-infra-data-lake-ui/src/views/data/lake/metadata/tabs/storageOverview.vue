<template>
  <div class="storage-overview">
    <div class="overview-header">
      <div class="title-group">
        <h2 class="section-title ">存储概览</h2>
        <p class="subtitle">显示当前目录/表的存储使用情况与最近活动</p>
      </div>
      <div class="actions">
        <el-button size="default" @click="handleRefresh" icon="Refresh">刷新</el-button>
        <el-button size="default" type="success" @click="handleSync" icon="Upload">同步</el-button>
      </div>
    </div>

    <div class="cards">
      <div class="card metric-card">
        <div class="metric-title">总存储</div>
        <div class="metric-value">{{ metrics.formattedTotalSize }}</div>
        <div class="metric-sub">包含 {{ metrics.totalObjects }} 项 (文件/表)</div>
      </div>

      <div class="card metric-card">
        <div class="metric-title">文件数</div>
        <div class="metric-value">{{ metrics.totalFiles }}</div>
        <div class="metric-sub">表/目录: {{ metrics.totalTables }}</div>
      </div>

      <div class="card metric-card">
        <div class="metric-title">最近同步</div>
        <div class="metric-value">{{ metrics.lastSync || '-' }}</div>
        <div class="metric-sub">状态: {{ metrics.syncStatus }}</div>
      </div>

      <div class="card metric-card">
        <div class="metric-title">冷热分布</div>
        <div class="metric-value">{{ metrics.hotPercent }}% 热数据</div>
        <div class="metric-sub">冷数据：{{ 100 - metrics.hotPercent }}%</div>
      </div>
    </div>

    <div class="main-panels">
      <div class="left-panel">
        <div class="panel card">
          <div class="panel-title">存储分层 / 格式分布</div>
          <div class="distribution">
            <div
              v-for="(item, idx) in distribution"
              :key="idx"
              class="dist-row"
            >
              <div class="dist-label">
                <span class="color-dot" :style="{ background: colors[idx % colors.length] }"></span>
                <span class="label-text">{{ item.name }}</span>
              </div>
              <div class="dist-bar-wrap">
                <el-progress
                  :text-inside="false"
                  :stroke-width="12"
                  :percentage="parseFloat(item.percent)"
                  :color="colors[idx % colors.length]"
                />
              </div>
              <div class="dist-value">{{ item.formattedSize }} · {{ item.percent }}%</div>
            </div>
          </div>
        </div>

        <div class="panel card">
          <div class="panel-title">最大存储对象</div>
          <el-table
            :data="largestObjects"
            stripe
            size="default"
            style="width: 100%;"
            v-loading="loading"
          >
            <el-table-column prop="rank" label="#" width="60">
              <template #default="{ row }">{{ row.rank }}</template>
            </el-table-column>
            <el-table-column prop="name" label="名称" min-width="200" />
            <el-table-column prop="formattedSize" label="大小" width="120" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="lastModified" label="最后修改" width="160" />
          </el-table>


        </div>
      <div class="pagination-wrap" v-if="total >= 0">
        <el-pagination
          background
          :page-size="pageSize"
          :current-page="page"
          :total="total"
          layout="prev, pager, next, sizes, jumper"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
      </div>

      <div class="right-panel">
        <div class="panel card">
          <div class="panel-title">最近活动</div>
          <el-timeline>
            <el-timeline-item
              v-for="(act, i) in recentActivity"
              :key="i"
              :timestamp="act.time"
              placement="top"
            >
              <div class="activity-title">{{ act.title }}</div>
              <div class="activity-desc">{{ act.desc }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div class="panel card">
          <div class="panel-title">分区/表简要</div>
          <div class="summary-list">
            <div class="summary-item" v-for="(s, i) in tableSummary" :key="i">
              <div class="s-left">
                <div class="s-name">{{ s.name }}</div>
                <div class="s-sub">{{ s.count }} 分区 · {{ s.formattedSize }}</div>
              </div>
              <div class="s-right">
                <el-button size="mini" @click="copyToClipboard(s.name)" type="info" plain>复制名</el-button>
              </div>
            </div>
          </div>
        </div>
      </div> <!-- right-panel -->
    </div> <!-- main-panels -->
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';

const props = defineProps({
  // 可选：传入 catalogId 或 tableId 来过滤具体目录/表
  catalogId: {
    type: [String, Number],
    default: null
  },
  // 可选：是否自动在 mount 时拉取
  autoFetch: {
    type: Boolean,
    default: true
  }
});

const loading = ref(false);

const page = ref(1);
const pageSize = ref(20);
const total = ref(100); // -1 表示未知

// 样式色带
const colors = ['#4caf50', '#2196f3', '#ff9800', '#9c27b0', '#f44336', '#00bcd4'];

// 下面的数据结构以通用字段为准，后端返回字段请按需适配
const metrics = ref({
  totalSize: 0,
  formattedTotalSize: '0 B',
  totalObjects: 0,
  totalFiles: 0,
  totalTables: 0,
  lastSync: '',
  syncStatus: '未知',
  hotPercent: 30
});

// 分布数组，每项 { name, size, formattedSize, percent }
const distribution = ref([]);

// 最大对象数组：{ rank, name, formattedSize, type, lastModified }
const largestObjects = ref([]);

// 最近活动：{ time, title, desc }
const recentActivity = ref([]);

// 表/分区摘要
const tableSummary = ref([]);

// 通用 fetch 地址（请按后端实际接口修改）
const fetchUrl = `/api/data/lake/storage/overview${props.catalogId ? '?catalogId=' + props.catalogId : ''}`;
// 同步地址（可选）
const syncUrl = `/api/data/lake/storage/sync${props.catalogId ? '?catalogId=' + props.catalogId : ''}`;

// 格式化字节为可读字符串
function formatBytes(bytes) {
  if (!bytes && bytes !== 0) return '-';
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
  if (bytes === 0) return '0 B';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const value = bytes / Math.pow(1024, i);
  return `${value.toFixed(value < 10 && i > 0 ? 2 : 1)} ${sizes[i]}`;
}

async function fetchData() {
  loading.value = true;
  try {
    // 最好后端提供稳定接口，这里用 fetch 做通用示例
    const res = await fetch(fetchUrl, { method: 'GET' });
    if (res.ok) {
      const data = await res.json();
      // 期望 data 结构：
      // { totalSize, totalObjects, totalFiles, totalTables, lastSync, syncStatus, hotPercent, distribution: [], largestObjects: [], recentActivity: [], tableSummary: [] }
      if (data && Object.keys(data).length > 0) {
        metrics.value.totalSize = data.totalSize || 0;
        metrics.value.formattedTotalSize = data.formattedTotalSize || formatBytes(metrics.value.totalSize);
        metrics.value.totalObjects = data.totalObjects || 0;
        metrics.value.totalFiles = data.totalFiles || 0;
        metrics.value.totalTables = data.totalTables || 0;
        metrics.value.lastSync = data.lastSync || '-';
        metrics.value.syncStatus = data.syncStatus || '正常';
        metrics.value.hotPercent = data.hotPercent != null ? data.hotPercent : 30;

        distribution.value = (data.distribution || []).map((d) => ({
          name: d.name,
          size: d.size,
          formattedSize: d.formattedSize || formatBytes(d.size || 0),
          percent: d.percent != null ? Number(d.percent).toFixed(1) : 0
        }));

        largestObjects.value = (data.largestObjects || []).map((it, idx) => ({
          rank: idx + 1,
          name: it.name,
          formattedSize: it.formattedSize || formatBytes(it.size || 0),
          type: it.type || '文件',
          lastModified: it.lastModified || '-'
        }));

        recentActivity.value = data.recentActivity || [];
        tableSummary.value = (data.tableSummary || []).map(s => ({
          name: s.name,
          count: s.partitionCount || s.count || 0,
          formattedSize: s.formattedSize || formatBytes(s.size || 0)
        }));

        loading.value = false;
        return;
      }
    }

    // 如果失败或返回不符合预期，则使用 mock 数据回退展示
    useMockData('接口返回异常或数据为空，已使用本地 mock 数据显示');
  } catch (err) {
    // 无后端或网络错误时回退到 mock 数据
    useMockData('请求存储概览失败，已使用本地 mock 数据显示');
    console.error('fetch storage overview error', err);
  } finally {
    loading.value = false;
  }
}

function useMockData(msg) {
  // 简单 mock 示例，可根据需要扩展
  metrics.value = {
    totalSize: 123456789012,
    formattedTotalSize: formatBytes(123456789012),
    totalObjects: 345,
    totalFiles: 320,
    totalTables: 25,
    lastSync: '2025-11-10 12:34',
    syncStatus: '成功',
    hotPercent: 42
  };

  distribution.value = [
    { name: 'Parquet', size: 70000000000, formattedSize: formatBytes(70000000000), percent: 56.7 },
    { name: 'ORC', size: 25000000000, formattedSize: formatBytes(25000000000), percent: 20.3 },
    { name: 'CSV', size: 9000000000, formattedSize: formatBytes(9000000000), percent: 7.3 },
    { name: '元数据', size: 12000000000, formattedSize: formatBytes(12000000000), percent: 15.7 }
  ];

  largestObjects.value = [
    { rank: 1, name: 'db1.table_big_2023', formattedSize: formatBytes(42000000000), type: '表', lastModified: '2025-11-10' },
    { rank: 2, name: 'db2.logs_2024', formattedSize: formatBytes(12000000000), type: '目录', lastModified: '2025-11-09' },
    { rank: 1, name: 'db1.table_big_2023', formattedSize: formatBytes(42000000000), type: '表', lastModified: '2025-11-10' },
    { rank: 2, name: 'db2.logs_2024', formattedSize: formatBytes(12000000000), type: '目录', lastModified: '2025-11-09' },
    { rank: 3, name: 'db3.events', formattedSize: formatBytes(8000000000), type: '表', lastModified: '2025-11-07' },
    { rank: 1, name: 'db1.table_big_2023', formattedSize: formatBytes(42000000000), type: '表', lastModified: '2025-11-10' },
    { rank: 1, name: 'db1.table_big_2023', formattedSize: formatBytes(42000000000), type: '表', lastModified: '2025-11-10' },
    { rank: 2, name: 'db2.logs_2024', formattedSize: formatBytes(12000000000), type: '目录', lastModified: '2025-11-09' },
    { rank: 3, name: 'db3.events', formattedSize: formatBytes(8000000000), type: '表', lastModified: '2025-11-07' },
    { rank: 3, name: 'db3.events', formattedSize: formatBytes(8000000000), type: '表', lastModified: '2025-11-07' }
  ];

  recentActivity.value = [
    { time: '2025-11-14 08:30', title: '表 db1.new_table 新增分区', desc: '新增分区 dt=2025-11-14, 大小 1.2 GB' },
    { time: '2025-11-13 20:15', title: '表 db2.table_removed 清理小文件', desc: '合并碎片并回收 300 MB' },
    { time: '2025-11-11 11:00', title: '同步任务完成', desc: '与对象存储同步完成，耗时 3 分钟' }
  ];

  tableSummary.value = [
    { name: 'db1.table_big_2023', count: 120, formattedSize: formatBytes(42000000000) },
    { name: 'db2.logs_2024', count: 365, formattedSize: formatBytes(12000000000) },
    { name: 'db1.table_big_2023', count: 120, formattedSize: formatBytes(42000000000) },
    { name: 'db3.events', count: 90, formattedSize: formatBytes(8000000000) },
  ];

  if (msg) ElMessage.info(msg);
}

async function handleRefresh() {
  await fetchData();
  ElMessage.success('已刷新存储概览');
}

async function handleSync() {
  // 调用同步接口（若后端提供），此处为示例调用
  try {
    loading.value = true;
    const res = await fetch(syncUrl, { method: 'POST' });
    if (res.ok) {
      const json = await res.json();
      if (json && (json.code === 200 || json.success === true || json.status === 'ok')) {
        ElMessage.success('发起同步成功，完成后请刷新查看最新数据');
      } else {
        ElMessage.success('已发起同步，请稍后查看结果');
      }
    } else {
      ElMessage.warning('同步请求已发送（响应非 200），请检查后端接口');
    }
  } catch (err) {
    console.error('sync error', err);
    ElMessage.error('同步请求失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function copyToClipboard(text) {
  if (!text) {
    ElMessage.warning('无可复制内容');
    return;
  }
  navigator.clipboard?.writeText(text).then(() => {
    ElMessage.success('复制成功');
  }).catch(() => {
    ElMessage.info('复制失败，尝试使用临时输入框复制');
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    ElMessage.success('复制成功');
  });
}

onMounted(() => {
  if (props.autoFetch) {
    fetchData();
  } else {
    useMockData();
  }
});

// 监听 catalogId 变化时重新拉取
watch(() => props.catalogId, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    fetchData();
  }
});
</script>

<style lang="scss" scoped>
.storage-overview {
  // padding: 14px;
  color: #333;

  .title-group {
    margin-left: 10px;
}

// 通用板块样式
.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 0px solid #f0f0f0;
}

  .overview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .title-group {
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
      }
      .subtitle {
        margin: 0;
        color: #888;
        font-size: 12px;
      }
    }

    .actions {
      display: flex;
      gap: 8px;
    }
  }

  .cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px;
    margin-bottom: 16px;

    .card {
      background: #fff;
      border-radius: 8px;
      padding: 14px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.04);
    }

    .metric-card {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .metric-title {
        font-size: 12px;
        color: #666;
      }
      .metric-value {
        font-size: 20px;
        font-weight: 700;
      }
      .metric-sub {
        font-size: 12px;
        color: #999;
      }
    }
  }

  .main-panels {
    display: flex;
    gap: 12px;

    .left-panel {
      flex: 2;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .right-panel {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .panel {
      padding: 12px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.04);

      .panel-title {
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 10px;
      }
    }

    .distribution {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .dist-row {
        display: grid;
        grid-template-columns: 160px 1fr 120px;
        align-items: center;
        gap: 10px;

        .dist-label {
          display: flex;
          align-items: center;
          gap: 8px;

          .color-dot {
            width: 12px;
            height: 12px;
            border-radius: 2px;
            display: inline-block;
          }

          .label-text {
            font-size: 13px;
            color: #333;
          }
        }

        .dist-bar-wrap {
          width: 100%;
        }

        .dist-value {
          text-align: right;
          font-size: 13px;
          color: #666;
        }
      }
    }

    .summary-list {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .summary-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 8px;
        border-radius: 6px;
        background: #fafafa;

        .s-left {
          .s-name {
            font-weight: 600;
            font-size: 13px;
            color: #333;
          }
          .s-sub {
            font-size: 12px;
            color: #888;
          }
        }

        .s-right {
        }
      }
    }

    .activity-title {
      font-weight: 600;
      font-size: 13px;
      color: #333;
    }
    .activity-desc {
      font-size: 12px;
      color: #777;
    }
  }

  @media (max-width: 900px) {
    .main-panels {
      flex-direction: column;

      .left-panel, .right-panel {
        width: 100%;
      }
    }
  }
}
</style>