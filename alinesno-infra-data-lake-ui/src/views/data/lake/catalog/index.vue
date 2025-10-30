<template>
  <div class="container">

    <el-row>
      <el-col :span="3">
        <SideTypePanel ref="sideTypePanelRef" @selectType="selectType" />
      </el-col>
      <el-col :span="21">

        <!-- 页面标题和筛选器 -->
        <div class="header-actions-row">
          <div class="page-header">
            <h1 class="page-title">领域管理</h1>
          </div>

          <div class="filter-controls">
            <!--  添加目录  -->
            <AddCatalogBtn ref="addCatalogBtnRef" @addSuccess="handleListCatalog" @updateSuccess="handleListCatalog" />
          </div>

        </div>

        <!-- 领域统计卡片 -->
        <!-- <StaticPanel v-if="total > 0" /> -->

        <div class="header-actions-row">
          <div class="page-header">
            <p class="page-description">管理和查看所有数据湖领域及其包含的表</p>
          </div>
        </div>

        <!-- 领域列表卡片 - 使用v-for循环渲染 -->
        <div class="projects-grid">
          <!-- 领域卡片 - 循环渲染 -->
          <div class="catalog-card" v-for="catalog in projects" :key="catalog.id">
            <div class="catalog-card-content">
              <p class="catalog-title" @click="enterCatalog(catalog)">
              <div class="catalog-icon">
                <i :class="catalog.icon"></i>
              </div>
              {{ catalog.catalogName }}
              </p>

              <p class="catalog-description">
                <el-text line-clamp="2" v-if="catalog.description">
                  {{ catalog.description }}
                </el-text>
                <el-text line-clamp="2" v-else>
                  暂无描述
                </el-text>
              </p>

              <div class="catalog-metrics">
                <div class="metric">
                  <div class="metric-label">
                    <i class="fa-regular fa-file-lines"></i> <!-- 替换原 fa-file-text-o -->
                    <span>数据表数</span>
                  </div>
                  <span class="metric-value">{{ catalog.tableCount }}</span>
                </div>

                <div class="metric">
                  <div class="metric-label">
                    <i class="fa-solid fa-database"></i> <!-- 6.x 中数据库图标类未变，但需确认使用 solid 风格 -->
                    <span>元数据量</span>
                  </div>
                  <span class="metric-value">{{ catalog.storageSize }}</span>
                </div>

                <!-- <div class="metric">
                  <div class="metric-label">
                    <i class="fa-regular fa-calendar"></i> 
                    <span>最近更新</span>
                  </div>
                  <span class="metric-value">{{ catalog.addTime }}</span>
                </div> -->
              </div>

            </div>

            <div class="catalog-footer">
              <span class="catalog-status" :class="catalog.status === '活跃' ? 'status-active' : 'status-archived'">
                {{ catalog.status }}
              </span>
              <span style="display: flex;flex-direction: row;gap: 5px;">
                <button class="view-details" @click="handleEdit(catalog)">
                  <i class="fa-solid fa-pen-nib"></i>
                  编辑
                </button>
                <el-popconfirm title="确定要删除吗？" @confirm="handleDelete(catalog)">
                  <template #reference>
                    <el-button type="info" text bg size="small" @click.stop>
                      <i class="fa-solid fa-trash"></i>&nbsp;删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </span>
            </div>
          </div>

        </div>
        <div v-if="total == 0">
          <el-empty description="当前暂无设置目录，请点击右上角添加行业数据目录"></el-empty>
        </div>

        <!-- 分页 -->
        <div class="pagination-container" v-if="total > 0">
          <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
            :page-sizes="[12, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange" @current-change="handleCurrentChange"></el-pagination>
        </div>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { ref } from 'vue';

import {
  listCatalog,
  deleteCatalog
} from '@/api/data/lake/catalog';

import SideTypePanel from './siderPanel.vue'
import AddCatalogBtn from './addCatalog.vue';
import StaticPanel from './staticPanel.vue';

const addCatalogBtnRef = ref(null);
const router = useRouter();

// 查询参数
const loading = ref(true)
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 12,
  searchKey: null
})

// 领域数据JSON
const projects = ref([]);

// 进入领域
const enterCatalog = (catalog) => {
  router.push({ path: "/lake/data/lake/metadata/index", query: { catalogId: catalog.id } });
}

// 获取项目列表
const handleListCatalog = () => {
  listCatalog(queryParams.value).then(res => {
    projects.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

// 分页处理
const handleSizeChange = (val) => {
  queryParams.value.pageNum = 1;
  queryParams.value.pageSize = val;
  handleListCatalog();
};

const handleCurrentChange = (val) => {
  queryParams.value.pageNum = val;
  handleListCatalog();
};

// 处理编辑操作
const handleEdit = (catalog) => {
  addCatalogBtnRef.value.editCatalog(catalog);
};

// 删除项目
const handleDelete = (item) => {
  deleteCatalog(item.id).then(res => {
    handleListCatalog()
  })
}

onMounted(() => {
  handleListCatalog()
})

</script>

<style lang="scss" scoped>
.container {
  width: 100%;
  padding: 10px;
  background: #fff;
  min-height: calc(100vh - 40px);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 24px;
}

.page-title {
  font-size: 18px; // clamp(1.5rem, 3vw, 2.5rem);
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 8px;
}

.page-description {
  color: #6b7280;
  font-size: 14px;
}

.header-actions-row {
  display: flex;
  flex-direction: column;
  gap: 16px;
  // margin-bottom: 12px;
}

@media (min-width: 768px) {
  .header-actions-row {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

.icon-primary {
  background-color: rgba(22, 93, 255, 0.1);
  color: #1d75b0;
}

.icon-secondary {
  background-color: rgba(54, 207, 201, 0.1);
  color: #36CFC9;
}

.icon-accent {
  background-color: rgba(114, 46, 209, 0.1);
  color: #722ED1;
}

.icon-success {
  background-color: rgba(82, 196, 26, 0.1);
  color: #1d75b0;
}

/* 领域列表 */
.projects-grid {
  display: grid;
  margin-top: 0px;
  grid-template-columns: 1fr;
  gap: 10px;
  margin-bottom: 40px;
}

@media (min-width: 768px) {
  .projects-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .projects-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.catalog-card {
  background-color: white;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(6, 7, 9, .1);
  transition: all 0.3s ease;
  cursor: pointer;
}

.catalog-card-content {
  padding: 14px;
}

.catalog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.catalog-icon {
  display: flex;
      align-items: center;
      justify-content: center;
      font-size: 17px;
      height: 30px;
      border-radius: 5px;
      width: 30px;
      color: #fff;
      background: #409eff;
}

.catalog-menu {
  color: #9ca3af;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.2s ease;
}

.catalog-menu:hover {
  color: #4b5563;
}

.catalog-title {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
  display: flex;
  gap: 5px;
  align-items: center;
  margin-top: 0px;
  margin-bottom: 10px;
}

.catalog-description {
  font-size: 14px;
  color: #6b7280;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  display: flex;
  gap: 10px;
  margin-top: 5px;
  margin-bottom: 10px;
  height: 35px;
}

.catalog-metrics {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.catalog-footer {
  padding: 8px 24px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.catalog-status {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.status-active {
  background-color: rgba(82, 196, 26, 0.1);
  color: #1d75b0;
}

.status-archived {
  background-color: rgba(229, 231, 235, 0.5);
  color: #6b7280;
}

.view-details {
  color: #1d75b0;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s ease;
  background: none;
  border: none;
  cursor: pointer;
}
</style>
