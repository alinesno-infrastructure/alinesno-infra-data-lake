<template>
  <div class="resource-overview">
    <div class="header">
      <div class="title title-group">
        <h2 class="section-title ">资源查询</h2>
        <p class="subtitle">编写 SQL 查询表数据（默认分页查询前1000条）</p>
      </div>
      <div class="actions">
        <!--
        <el-button size="default" @click="resetSql">重置SQL</el-button>
        <el-button size="default" @click="copySql">复制SQL</el-button>
        -->
        <el-button size="default" type="primary" @click="runQuery(true)" :loading="loading">运行</el-button>
        <el-button size="default" @click="downloadCsv" :disabled="!rows.length">导出CSV</el-button>
        <el-button size="default" type="warning" @click="explainSql" :loading="explaining">Explain</el-button>
      </div>
    </div>

    <div class="editor">
      <el-input
        v-model="sqlText"
        size="large"
        resize="none"
        placeholder="请输入 SQL（如：SELECT * FROM schema.table WHERE ...）"
        clearable
      />
    </div>

    <div class="meta">
      <div class="meta-left">
        <el-tag type="info" v-if="currentTableLabel">表: {{ currentTableLabel }}</el-tag>
        <el-tag v-else>未选择表，手动输入 SQL</el-tag>
      </div>
      <div class="meta-right">
        <span class="small-text">总计: {{ total }}</span>
      </div>
    </div>

    <div class="result-panel card">
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        style="width:100%"
        :empty-text="loading ? '加载中...' : (rows.length ? '无数据' : '无结果')"
        @row-dblclick="handleRowDblClick"
      >
        <el-table-column
          v-for="(col, idx) in columns"
          :key="col.key || col"
          :prop="col.key || col"
          :label="col.title || prettify(col)"
          :min-width="colMinWidth(col, idx)"
        >
          <template #default="{ row }">
            <span class="cell-text">{{ formatCell(row[col.key || col]) }}</span>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog title="Explain 结果" v-model="showExplain" width="60%">
      <pre class="explain-pre">{{ explainText }}</pre>
      <template #footer>
        <el-button @click="showExplain = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog title="行详情" v-model="showRowDetail" width="50%">
      <div class="row-detail">
        <pre>{{ JSON.stringify(currentRow, null, 2) }}</pre>
      </div>
      <template #footer>
        <el-button @click="showRowDetail = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { query as apiQuery, explain as apiExplain } from '@/api/data/lake/webQuery';

// Props: 接收当前表对象（可以包含 catalogName/schemaName/tableName 或 id）
const props = defineProps({
  currentCatalogTable: {
    type: Object,
    default: null
  }
});

const loading = ref(false);
const explaining = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(-1); // -1 表示未知
const sqlText = ref('');
const columns = ref([]); // array of column names or { key, title }
const rows = ref([]);
const showExplain = ref(false);
const explainText = ref('');
const showRowDetail = ref(false);
const currentRow = ref(null);

// 构造当前表 label
const currentTableLabel = computed(() => {
  const t = props.currentCatalogTable;
  if (!t) return '';
  const parts = [];
  if (t.catalogName) parts.push(t.catalogName);
  if (t.schemaName || t.database) parts.push(t.schemaName || t.database);
  if (t.tableName) parts.push(t.tableName);
  return parts.join('.');
});

// 根据 currentCatalogTable 构造默认 SQL
function buildDefaultSql() {
  const t = props.currentCatalogTable;

  console.log('t = ' + JSON.stringify(t));

  if (t && t.statistics && (t.databaseName || t.tableName)) {
    const catalog = t.catalogName ? `${t.catalogName}.` : '' ;
    const schema = t.statistics.databaseName ? `${t.statistics.databaseName}.` : '' ;
    const table = t.statistics.tableName ;
    return `SELECT * FROM ${catalog}${schema}${table} LIMIT ${pageSize.value} OFFSET ${(page.value - 1) * pageSize.value}`;
  }
  return '';
}

// prettify 列名作为标题
function prettify(col) {
  if (!col) return '';
  return String(col).replace(/[_\.]/g, ' ').replace(/\b\w/g, s => s.toUpperCase());
}

// 格式化单元格显示（简单处理 null/对象）
function formatCell(val) {
  if (val === null || val === undefined) return '-';
  if (typeof val === 'object') return JSON.stringify(val);
  return String(val);
}

function colMinWidth(col, idx) {
  return idx === 0 ? 120 : 160;
}

// 重置 SQL 到默认
function resetSql() {
  sqlText.value = buildDefaultSql();
}

// 监听 table 变化，自动重置 SQL 并执行默认查询
watch(() => props.currentCatalogTable, (newVal) => {
  page.value = 1;
  resetSql();
  if (newVal && (newVal.tableName || newVal.name)) {
      sqlText.value = buildDefaultSql();

      if(sqlText.value){
        console.log('sqlText = ' + sqlText.value) ;
        runQuery();
      }
  } else {
    rows.value = [];
    columns.value = [];
    total.value = -1;
  }
}, { immediate: true });

// 主查询方法
async function runQuery(resetPage = false) {
  if (resetPage) {
    page.value = 1;
  }

  if (!sqlText.value || sqlText.value.trim() === '') {
    sqlText.value = buildDefaultSql();
  }

  let finalSql = sqlText.value.trim();
  const hasLimitOffset = /limit\s+\d+/i.test(finalSql) || /offset\s+\d+/i.test(finalSql);
  if (!hasLimitOffset) {
    finalSql = `${finalSql} LIMIT ${pageSize.value} OFFSET ${(page.value - 1) * pageSize.value}`;
  }

  loading.value = true;
  try {
    const payload = {
      sql: finalSql,
      page: page.value,
      pageSize: pageSize.value,
      tableContext: props.currentCatalogTable || null
    };

    const res = await apiQuery(payload);
    // 兼容：request 工具通常返回 { data: ... } 或 直接返回 data，取决于实现
    const data = res && res.data ? res.data : res;

    if (data && (Array.isArray(data.rows) || Array.isArray(data.data))) {
      const resultRows = data.rows || data.data;
      rows.value = resultRows;
      if (Array.isArray(data.columns) && data.columns.length) {
        columns.value = data.columns.map(c => (typeof c === 'string' ? c : (c.name || c.field)));
      } else if (resultRows.length) {
        columns.value = Object.keys(resultRows[0]);
      } else {
        columns.value = [];
      }
      total.value = typeof data.total === 'number' ? data.total : (resultRows.length < pageSize.value ? ((page.value - 1) * pageSize.value + resultRows.length) : -1);
      return;
    }

    if (data && data.data && Array.isArray(data.data)) {
      rows.value = data.data;
      columns.value = data.columns || (rows.value[0] ? Object.keys(rows.value[0]) : []);
      total.value = data.total || rows.value.length;
      return;
    }

    if (data && data.error) {
      ElMessage.error(`查询失败: ${data.error}`);
    } else {
      ElMessage.warning('查询返回异常结果，使用 mock 数据展示');
      useMock();
    }
  } catch (err) {
    console.error('query error', err);
    ElMessage.error('查询异常，已使用本地 mock 数据回退');
    useMock();
  } finally {
    loading.value = false;
  }
}

// 简单 Explain（如果后端支持）
async function explainSql() {
  if (!sqlText.value) {
    ElMessage.warning('没有 SQL 可 Explain');
    return;
  }
  explaining.value = true;
  try {
    const payload = { sql: sqlText.value, tableContext: props.currentCatalogTable || null };
    const res = await apiExplain(payload);
    // res 可能是字符串或 response 对象
    const txt = res && res.data ? res.data : res;
    explainText.value = txt || '无 Explain 输出';
  } catch (err) {
    explainText.value = 'Explain 请求失败: ' + String(err);
  } finally {
    explaining.value = false;
    showExplain.value = true;
  }
}

// 下载 CSV
function downloadCsv() {
  if (!rows.value.length || !columns.value.length) {
    ElMessage.warning('无可导出数据');
    return;
  }
  const header = columns.value;
  const csvRows = [];
  csvRows.push(header.map(h => `"${String(h).replace(/"/g, '""')}"`).join(','));
  for (const r of rows.value) {
    const cells = header.map(h => {
      const v = r[h];
      if (v === null || v === undefined) return '';
      if (typeof v === 'object') return `"${JSON.stringify(v).replace(/"/g, '""')}"`;
      return `"${String(v).replace(/"/g, '""')}"`;
    });
    csvRows.push(cells.join(','));
  }
  const blob = new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  const fileName = `query_result_${Date.now()}.csv`;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  ElMessage.success('已下载 CSV');
}

// 复制 SQL 到剪贴板
function copySql() {
  if (!sqlText.value) {
    ElMessage.warning('无 SQL 可复制');
    return;
  }
  navigator.clipboard?.writeText(sqlText.value).then(() => {
    ElMessage.success('复制成功');
  }).catch(() => {
    const ta = document.createElement('textarea');
    ta.value = sqlText.value;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    ElMessage.success('复制成功');
  });
}

// 分页回调
function handlePageChange(p) {
  page.value = p;
  runQuery(false);
}
function handleSizeChange(size) {
  pageSize.value = size;
  page.value = 1;
  runQuery(true);
}

// 双击行显示详情
function handleRowDblClick(row) {
  currentRow.value = row;
  showRowDetail.value = true;
}

// Mock 数据回退
function useMock() {
  columns.value = ['id', 'name', 'value', 'created_at'];
  rows.value = [
    { id: 1, name: 'Alice', value: 12.34, created_at: '2025-11-01' },
    { id: 2, name: 'Bob', value: 23.45, created_at: '2025-11-02' },
    { id: 3, name: 'Carol', value: 34.56, created_at: '2025-11-03' }
  ];
  total.value = 3;
}

onMounted(() => {
  sqlText.value = buildDefaultSql();
});
</script>

<style lang="scss" scoped>
.resource-overview {
  color: #333;

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 0px solid #f0f0f0;
}

.title-group {
    margin-left: 10px;
}

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    .title {
      h3 { margin: 0; font-size: 16px; font-weight: 700; }
      .subtitle { margin: 0; font-size: 12px; color: #888; }
    }

    .actions {
      display: flex;
      gap: 8px;
    }
  }

  .editor {
    margin-bottom: 10px;
    .el-input__inner {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, "Roboto Mono", "Courier New", monospace;
      font-size: 13px;
    }
  }

  .meta {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    align-items: center;

    .small-text { color: #666; font-size: 12px; }
  }

  .result-panel {
    background: #fff;
    border-radius: 8px;
    padding: 12px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.04);

    .pagination-wrap {
      margin-top: 12px;
      display: flex;
      justify-content: flex-end;
    }

    .cell-text {
      display: inline-block;
      max-width: 360px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .explain-pre {
    white-space: pre-wrap;
    max-height: 60vh;
    overflow: auto;
    background: #f8f8f8;
    padding: 12px;
    border-radius: 6px;
  }

  .row-detail pre {
    max-height: 50vh;
    overflow: auto;
    background: #fafafa;
    padding: 12px;
    border-radius: 6px;
  }
}
</style>