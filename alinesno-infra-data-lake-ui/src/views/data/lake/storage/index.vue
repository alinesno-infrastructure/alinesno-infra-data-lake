<template>
  <div class="app-container">
    <el-row>
      <el-col :span="3">
        <SideTypePanel ref="sideTypePanelRef" @selectType="selectType" />
      </el-col>
      <el-col :span="21">
        <main class="main-content">
          <!-- 操作按钮区 -->
          <div class="action-bar">
            <div class="action-buttons">

              <el-button 
                type="warning"
                text bg
                id="upload-btn" 
                @click="showCatalogUploadModal = true"
                icon="Upload"
              >
                  业务域文件上传
              </el-button>

              <el-button 
                type="primary"
                text bg
                id="upload-btn" 
                @click="showUploadModal = true"
                icon="Upload"
              >
                上传文件
              </el-button>
              <el-button 
                type="danger"
                text bg
                class="default-btn"
                icon="FolderAdd"
                @click="startCreateFolder"
              >
                新建文件夹
              </el-button>
            </div>
            
            <div class="action-controls">
              <el-select 
                v-model="sortOption" 
                class="sort-selector"
                @change="handleSortChange"
              >
                <el-option 
                  v-for="option in sortOptions" 
                  :key="option.value" 
                  :label="option.label" 
                  :value="option.value"
                ></el-option>
              </el-select>
            </div>

          </div>
          
          <!-- 存储空间使用情况卡片 -->
          <div class="stats-cards">
            <el-card class="stat-card" shadow="none">
              <div class="stat-info">
                <p class="stat-label">总存储</p>
                <h3 class="stat-value">{{ fileStats.totalSize }}</h3>
              </div>
              <div class="stat-trend up-trend">
                <i class="fa fa-arrow-up"></i> 较上月增长 5.2%
              </div>
            </el-card>
            
            <el-card class="stat-card" shadow="none">
              <div class="stat-info">
                <p class="stat-label">文件总数</p>
                <h3 class="stat-value">{{ fileStats.totalFiles }}</h3>
              </div>
              <div class="stat-trend up-trend">
                <i class="fa fa-arrow-up"></i> 较上月增长 12.8%
              </div>
            </el-card>
            
            <el-card class="stat-card" shadow="none">
              <div class="stat-info">
                <p class="stat-label">今日文件数</p>
                <h3 class="stat-value">{{ fileStats.todayUploads }}</h3>
              </div>
              <div class="stat-trend up-trend">
                <i class="fa fa-arrow-up"></i> 较上月增长 12.8%
              </div>
            </el-card>
            
            <el-card class="stat-card" shadow="none">
              <div class="stat-info">
                <p class="stat-label">本周文件数</p>
                <h3 class="stat-value">{{ fileStats.weekUploads }}</h3>
              </div>
              <div class="stat-trend down-trend">
                <i class="fa fa-arrow-down"></i> 较上月减少 3.4%
              </div>
            </el-card>

          </div>
          
          <!-- 当前路径面包屑 -->
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>
                <el-button 
                  type="text" 
                  class="breadcrumb-item-btn"
                  @click="navigateToIndex(-1)"
                >
                  根目录
                </el-button>
              </el-breadcrumb-item>
              <el-breadcrumb-item 
                v-for="(item, index) in pathHistory" 
                :key="item.id || index"
              >
                <el-button 
                  type="text" 
                  class="breadcrumb-item-btn"
                  @click="navigateToIndex(index)"
                >
                  {{ item.fileName }}
                </el-button>
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <!-- 文件列表 -->
          <!-- <h3 class="section-title">文件</h3> -->
          <el-table 
            :data="displayedItems" 
            class="files-table"
            :header-cell-style="{ 
              'background-color': '#f9fafb', 
              'color': '#4e5969',
              'font-size': '12px',
              'font-weight': '500'
            }"
          >
            <el-table-column 
              prop="name" 
              label="名称" 
              class-name="name-column"
            >
              <template #default="scope">
                <!-- 新建文件夹输入框 -->
                <div v-if="scope.row.isCreating" class="folder-create-item">
                  <div class="file-icon warning-bg">
                    <i class="fa fa-folder"></i>
                  </div>
                  <el-input
                    v-model="newFolderName"
                    ref="folderInputRef"
                    size="small"
                    placeholder="输入文件夹名称"
                    @keyup.enter="confirmCreateFolder"
                    @keyup.escape="cancelCreateFolder"
                    class="folder-name-input"
                  />
                  <div class="folder-actions">
                    <el-button 
                      type="success" 
                      size="small" 
                      icon="Check"
                      @click="confirmCreateFolder"
                      circle
                    ></el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      icon="Close"
                      @click="cancelCreateFolder"
                      circle
                    ></el-button>
                  </div>
                </div>
                
                <!-- 正常文件/文件夹显示 -->
                <div v-else class="file-item" @click="handleItemClick(scope.row)">
                  <div class="file-icon" :class="scope.row.typeInfo ? scope.row.typeInfo.colorClass : 'primary-bg'">

                    <i v-if="getFileType(scope.row.fileExtension) && scope.row.isDirectory === 0 " :class="getFileType(scope.row.fileExtension).icon + ' ' + getFileType(scope.row.fileExtension).colorClass"></i>
                    <i v-else :class="scope.row.typeInfo ? scope.row.typeInfo.icon : 'fa fa-folder'"></i>
                  </div>
                  <div class="file-info">
                    <p class="file-name">
                      {{ scope.row.fileName }}</p>
                    <p v-if="scope.row.typeInfo" class="file-type">
                      {{ scope.row.typeInfo.category }} · {{ scope.row.typeInfo.fileExtension }}
                    </p>
                  </div>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column 
              prop="addTime" 
              width="200"
              label="修改日期" 
              class-name="date-column"
            ></el-table-column>
            
            <el-table-column 
              prop="size" 
              align="center"
              width="130"
              label="大小" 
              class-name="size-column"
            >
              <template #default="scope">
                <span v-if="scope.row.isDirectory === 0">
                  {{ formatFileSize(scope.row.fileSize) || '-' }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            
            <el-table-column 
              label="操作" 
              width="200"
              align="center"
              class-name="action-column"
            >
              <template #default="scope">
                <div v-if="!scope.row.isCreating" class="file-actions">
                  <el-button 
                    v-if="!scope.row.isFolder"
                    icon="CopyDocument" 
                    type="success"
                    text
                    bg
                    title="复制"
                    @click="copyLink(scope.row)"
                  ></el-button>
                  <el-button 
                    v-if="!scope.row.isFolder"
                    icon="Download" 
                    type="primary"
                    text
                    bg
                    title="下载"
                    @click="downloadFile(scope.row)"
                  ></el-button>
                  <el-button 
                    icon="Delete" 
                    type="danger"
                    text
                    bg
                    title="删除"
                    @click="deleteItem(scope.row)"
                  ></el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <div class="table-pagination">
            <div class="pagination-info">显示 {{ (queryParams.pageNum  - 1) * queryParams.pageSize + 1 }}-{{ Math.min(queryParams.pageNum * queryParams.pageSize, displayedItems.length) }} 条，共 {{ total }} 条</div>

            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[5, 10, 20]"
              :small="false"
              background
              :total="total"
              :pager-count="queryParams.pageSize"
					    layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </main>
      </el-col>
    </el-row>

    <!-- 遮罩层 -->
    <!-- <div class="sidebar-overlay" v-if="sidebarVisible" @click="toggleSidebar"></div> -->
    
    <!-- 上传文件模态框 -->
    <UploadModalPanel 
      :showUploadModal="showUploadModal"
      @uploadSuccess="loadFiles"
      @update:showUploadModal="showUploadModal = $event"
      :currentDirectory="currentDirectory"
      :fileTypes="fileTypes"
    />

    <!-- 业务域上传文件模态框 -->
    <CatalogUploadModalPanel 
      :showCatalogUploadModal="showCatalogUploadModal"
      @uploadSuccess="loadFiles"
      @update:showCatalogUploadModal="showCatalogUploadModal = $event"
      :currentDirectory="currentDirectory"
      :fileTypes="fileTypes"
    />

  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import SideTypePanel from './siderPanel.vue';

import UploadModalPanel from './uploadModal.vue';
import CatalogUploadModalPanel from './catalogUploadModal.vue';

import { 
  createFolder, 
  deleteFolder, 
  statistics,
  listFiles 
} from '@/api/data/lake/cloudFile';

// 侧边栏状态
const sidebarVisible = ref(false);

// 排序选项
const sortOptions = ref([
  { label: '最近修改时间', value: 'modifiedTime' },
  { label: '名称', value: 'name' },
  { label: '大小', value: 'size' },
  { label: '类型', value: 'type' }
]);
const sortOption = ref('modifiedTime');

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  parentId: 0 
}) ;

// 分页控制
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(queryParams.pageSize);

// 上传相关状态
const showUploadModal = ref(false);
const showCatalogUploadModal = ref(false);

// 文件夹相关状态
const isCreatingFolder = ref(false);
const newFolderName = ref('');
const folderInputRef = ref(null);
const currentDirectory = ref(null);

// 路径历史记录 - 使用对象数组存储，每个对象包含id和fileName
const pathHistory = ref([]);

// 文件类型定义 JSON 数据
const fileTypes = ref([
  {"extension":"png","category":"图片","icon":"fa-solid fa-image","colorClass":"primary-bg"},
  {"extension":"jpg","category":"图片","icon":"fa-solid fa-image","colorClass":"primary-bg"},
  {"extension":"jpeg","category":"图片","icon":"fa-solid fa-image","colorClass":"primary-bg"},
  {"extension":"pdf","category":"文档","icon":"fa-solid fa-file-pdf","colorClass":"danger-bg"},
  {"extension":"docx","category":"文档","icon":"fa-solid fa-file-word","colorClass":"blue-bg"},
  {"extension":"doc","category":"文档","icon":"fa-solid fa-file-word","colorClass":"blue-bg"},
  {"extension":"xlsx","category":"表格","icon":"fa-solid fa-file-excel","colorClass":"green-bg"},
  {"extension":"xls","category":"表格","icon":"fa-solid fa-file-excel","colorClass":"green-bg"},
  {"extension":"pptx","category":"演示文稿","icon":"fa-solid fa-file-powerpoint","colorClass":"orange-bg"},
  {"extension":"ppt","category":"演示文稿","icon":"fa-solid fa-file-powerpoint","colorClass":"orange-bg"},
  {"extension":"mp4","category":"视频","icon":"fa-solid fa-file-video","colorClass":"purple-bg"},
  {"extension":"avi","category":"视频","icon":"fa-solid fa-file-video","colorClass":"purple-bg"},
  {"extension":"mp3","category":"音频","icon":"fa-solid fa-file-audio","colorClass":"warning-bg"},
  {"extension":"zip","category":"压缩包","icon":"fa-solid fa-file-archive","colorClass":"gray-bg"},
  {"extension":"rar","category":"压缩包","icon":"fa-solid fa-file-archive","colorClass":"gray-bg"},
  {"extension":"md","category":"文档","icon":"fa-solid fa-file-code","colorClass":"info-bg"}, // 新增markdown类型
  {"extension":"default","category":"未知","icon":"fa-solid fa-file","colorClass":"secondary-bg"} // 未找到的默认类型
]);

// 通过后缀匹配文件类型
const getFileType = (extension) => {
  const type = fileTypes.value.find(type => type.extension === extension);
  return type || fileTypes.value.find(type => type.extension === 'default');
};

// 文件数据 - 统一存储，通过 isFolder 属性区分
const files = ref([]);

// 文件统计信息
const fileStats = ref({
  totalFiles: 0,
  sharedFiles: 0
});

// 计算属性：当前路径下的显示项
const displayedItems = computed(() => {
  let items = [...files.value]; // 创建副本避免直接修改原数组
  
  // 如果正在创建文件夹，添加创建项
  if (isCreatingFolder.value) {
    items = [...items]; // 确保响应式更新
    items.unshift({
      isCreating: true,
      creating: true, // 添加额外标识
      isFolder: true
    });
  }
  
  // 排序逻辑
  return items;
});

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 导航到指定索引的路径
const navigateToIndex = (index) => {
  if (index === -1) {
    // 回到根目录
    currentDirectory.value = null;
    queryParams.value.parentId = 0;
    pathHistory.value = [];
  } else {
    // 导航到指定层级
    currentDirectory.value = pathHistory.value[index];
    queryParams.value.parentId = currentDirectory.value.id;
    // 截断历史记录到当前点击的位置
    pathHistory.value = pathHistory.value.slice(0, index + 1);
  }
  
  loadFiles(); // 加载新路径的文件
};

// 加载文件列表
const loadFiles = async () => {
  try {

    handleStatistics();
    const response = await listFiles(queryParams.value);
    
    if (response.code === 200) {
      // 根据实际API响应结构调整
      files.value = response.rows || response.data || [];
      total.value = response.total ; 
      
      // 更新文件统计
      fileStats.value.totalFiles = response.total  ; 
      fileStats.value.sharedFiles = files.value.filter(file => file.shared).length;
    } else {
      ElMessage.error('加载文件列表失败');
    }
  } catch (error) {
    console.error('加载文件列表错误:', error);
    ElMessage.error('加载文件列表失败');
  }
};

// 开始创建文件夹
const startCreateFolder = () => {
  isCreatingFolder.value = true;
  newFolderName.value = '新建文件夹';
  
  nextTick(() => {
    if (folderInputRef.value) {
      folderInputRef.value.focus();
      folderInputRef.value.select();
    }
  });
};

// 复制下载链接
const copyLink = (row) => {

  // 如果是文件夹则不允许复制 
  // if (row.isDirectory == 1) {
  //   ElMessage.warning('文件夹不支持复制下载链接');
  //   return;
  // }

  const fileStorageId = row.id ; 

  navigator.clipboard.writeText(fileStorageId)
    .then(() => {
      // 复制成功提示
      ElMessage.success('复制成功');
    })
    .catch((err) => {
      // 复制失败处理（兼容旧浏览器）
      console.error('复制失败:', err);
      // 降级方案：使用文本框临时存储并复制
      const textarea = document.createElement('textarea');
    })

}

// 确认创建文件夹
const confirmCreateFolder = async () => {
  if (!newFolderName.value.trim()) {
    ElMessage.warning('请输入文件夹名称');
    return;
  }
  
  try {
    const createData = {
      folderName: newFolderName.value.trim(),
      parentId: currentDirectory.value?.id  || 0
    };
    
    const response = await createFolder(createData);
    
    if (response.code === 200) {
      ElMessage.success(`文件夹"${newFolderName.value.trim()}"创建成功`);
      await loadFiles(); // 重新加载文件列表
    } else {
      ElMessage.error('文件夹创建失败');
    }
  } catch (error) {
    console.error('创建文件夹错误:', error);
    ElMessage.error('文件夹创建失败');
  }
  
  // 重置创建状态
  cancelCreateFolder();
};

// 取消创建文件夹
const cancelCreateFolder = () => {
  isCreatingFolder.value = false;
  newFolderName.value = '';

  // 强制重新计算 displayedItems
  files.value = [...files.value];
};

// 处理项目点击（进入文件夹）
const handleItemClick = (item) => {
  if (!item) { // 回到根目录
    navigateToIndex(-1);
    return;
  }

  if (item.isDirectory == 1) {
    // 检查是否已在历史记录中
    const existingIndex = pathHistory.value.findIndex(path => path.id === item.id);
    
    if (existingIndex !== -1) {
      // 如果已存在，导航到该位置
      navigateToIndex(existingIndex);
    } else {
      // 如果不存在，添加到历史记录
      if (currentDirectory.value) {
        // 确保当前目录已在历史记录中
        const currentIndex = pathHistory.value.findIndex(path => path.id === currentDirectory.value.id);
        if (currentIndex === -1) {
          pathHistory.value.push({...currentDirectory.value});
        }
      }
      
      // 添加新目录到历史记录
      pathHistory.value.push({
        id: item.id,
        fileName: item.fileName
      });
      
      currentDirectory.value = item;
      queryParams.value.parentId = item.id;
      loadFiles();
    }
  }
};

// 删除项目
const deleteItem = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要${item.isFolder ? '删除文件夹"' + item.fileName + '"及其所有内容' : '删除文件"' + item.fileName + '"'}吗？`,
      '提示', 
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    const response = await deleteFolder(item.id);
    
    if (response.code === 200) {
      ElMessage.success(`${item.isFolder ? '文件夹' : '文件'}删除成功`);
      await loadFiles(); // 重新加载文件列表
    } else {
      ElMessage.error(`${item.isFolder ? '文件夹' : '文件'}删除失败`);
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除错误:', error);
      ElMessage.error('删除失败');
    }
  }
};

// 处理排序变化
const handleSortChange = (value) => {
  sortOption.value = value;
  loadFiles(); // 重新加载排序后的文件
};

// 分页处理
const handleSizeChange = (val) => {
  queryParams.value.pageNum = 1;
  queryParams.value.pageSize = val;
  loadFiles();
};

const handleCurrentChange = (val) => {
  queryParams.value.pageNum = val ;
  loadFiles();
};

// 文件操作
const downloadFile = (file) => {
  console.log('下载文件:', file.name);
  ElMessage.info(`开始下载: ${file.name}`);
};

const selectType = (type) => {
  console.log('选择文件类型:', type);
};

// 获取到统计数据
const handleStatistics = () => {
  // 获取文件统计数据
  statistics(currentDirectory.value?.id).then(response => {
    if (response.code === 200) {
      fileStats.value = response.data;
    }
  });
};

// 初始化逻辑
onMounted(() => {
  loadFiles();
  console.log('主组件初始化完成');
});
</script>

<style lang="scss" scoped>
// 基础变量定义
$primary-color: #2c6ecb;
$primary-light: rgba(44, 110, 203, 0.1);
$primary-hover: rgba(44, 110, 203, 0.05);

$success-color: #1d75b0;
$success-light: rgba(0, 180, 42, 0.1);

$warning-color: #ff7d00;
$warning-light: rgba(255, 125, 0, 0.1);

$danger-color: #f53f3f;
$danger-light: rgba(245, 63, 63, 0.1);

$text-primary: #1d2129;
$text-secondary: #4e5969;
$text-tertiary: #86909c;

$border-color: #e5e6eb;
$bg-color: #fafafa;
$card-bg: #ffffff;

$shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.06);
$shadow-md: 0 2px 8px rgba(0, 0, 0, 0.08);

$radius-sm: 4px;
$radius-md: 6px;
$radius-lg: 8px;
$radius-xl: 12px;

// 容器样式
.app-container {
  background: #fafafa;
  padding: 10px;
  box-sizing: border-box;
}

// 主内容区域
.main-content {
  box-sizing: border-box;
  min-height: calc(100vh - 60px);
  background: #fff;
  padding: 10px;
  border-radius: 8px;
}

// 操作栏样式
.action-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 10px 0;
  padding-bottom: 8px;

  .action-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .action-controls {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

// 排序选择器
.sort-selector {
  width: 180px;
}

// 统计卡片区域
.stats-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-bottom: 24px;

  @media (min-width: 768px) {
    grid-template-columns: repeat(4, 1fr);
  }

  .stat-card {
    background-color: $card-bg;
    border-radius: $radius-lg;
    padding: 0px;
    border: 1px solid $border-color;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      box-shadow: $shadow-md;
    }

    .stat-info {
      margin-bottom: 8px;

      .stat-label {
        font-size: 14px;
        color: $text-tertiary;
        margin: 0 0 4px 0;
      }

      .stat-value {
        font-size: 24px;
        font-weight: 600;
        color: $text-primary;
        margin: 0;
      }
    }

    .stat-trend {
      font-size: 12px;
      display: flex;
      align-items: center;
      gap: 4px;

      &.up-trend {
        color: $success-color;
      }

      &.down-trend {
        color: $danger-color;
      }
    }
  }
}

// 面包屑样式
.breadcrumb {
  margin-bottom: 16px;
  padding: 6px ; 
  background: $card-bg;
  border-radius: $radius-md;
  border: 1px solid $border-color;
  
  .breadcrumb-item {
    cursor: pointer;
    font-size: 14px;
    
    &:hover {
      color: $primary-color;
    }
    
    &.current-path {
      color: $text-primary;
      cursor: default;
      
      &:hover {
        color: $text-primary;
      }
    }
  }
}

// 区域标题
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 16px 0;
  padding-left: 4px;
}

// 文件表格样式
.files-table {
  margin-bottom: 16px;
  border-radius: $radius-lg;
  overflow: hidden;

  .el-table__row {
    transition: background-color 0.2s ease;

    &:hover > td {
      background-color: $primary-hover !important;
    }
  }

  // 文件夹创建项样式
  .folder-create-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 0;
    
    .folder-name-input {
      width: 200px;
      margin-right: 8px;
    }
    
    .folder-actions {
      display: flex;
      gap: 4px;
    }
  }

  .file-item {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    padding: 0px 0;

    .file-icon {
      width: 32px;
      height: 32px;
      border-radius: $radius-sm;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      flex-shrink: 0;
    }

    .file-info {
      min-width: 0;

      .file-name {
        font-size: 14px;
        color: $text-primary;
        margin: 0 0 2px 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .file-type {
        font-size: 12px;
        color: $text-tertiary;
        margin: 0;
      }
    }
    
    &:hover {
      .file-name {
        color: $primary-color;
      }
    }
  }

  .file-actions {
    display: flex;
    gap: 4px;
    justify-content: center;

    .action-btn {
      width: 28px;
      height: 28px;
      padding: 0;
      color: $text-tertiary;
      transition: all 0.2s ease;

      &:hover {
        color: $primary-color;
        background-color: $primary-light;
      }
    }
  }
}

// 分页样式
.table-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  font-size: 12px;
  color: $text-tertiary;

  .pagination-info {
    margin-right: 16px;
  }

  .el-pagination {
    display: flex;
    align-items: center;
  }
}

// 遮罩层样式
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

:deep(.el-card__body){
  padding: 10px !important;
}

// 颜色辅助类
.primary-bg {
  background-color: $primary-light;
  color: $primary-color;
}

.success-bg {
  background-color: $success-light;
  color: $success-color;
}

.warning-bg {
  background-color: $warning-light;
  color: $warning-color;
}

.danger-bg {
  background-color: $danger-light;
  color: $danger-color;
}

.blue-bg {
  background-color: rgba(64, 128, 255, 0.1);
  color: #4080ff;
}

.green-bg {
  background-color: rgba(0, 180, 42, 0.1);
  color: $success-color;
}

.purple-bg {
  background-color: rgba(114, 46, 209, 0.1);
  color: #722ed1;
}

.orange-bg {
  background-color: rgba(255, 153, 0, 0.1);
  color: $warning-color;
}

.gray-bg {
  background-color: rgba(134, 144, 156, 0.1);
  color: $text-tertiary;
}
</style>