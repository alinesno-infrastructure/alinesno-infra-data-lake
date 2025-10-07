<template>
  <el-dialog 
    :model-value="showCatalogUploadModal" 
    title=" 业务域文件上传文件" 
    :width="700"
    :close-on-click-modal="false"
    @close="closeUploadModal"
   >
    <div class="modal-body">
      <!-- 拖放区域 -->
      <div class="drop-area" ref="dropArea" 
           @dragenter.prevent="handleDragEnter"
           @dragover.prevent="handleDragOver"
           @dragleave.prevent="handleDragLeave"
           @drop.prevent="handleDrop"
           >
        <div class="drop-icon">
          <i class="fa fa-cloud-upload"></i>
        </div>
        <p class="drop-text">拖放文件到此处上传</p>
        <p class="drop-subtext">支持图片、文档、音视频等格式</p>
        <label class="select-file-btn">
          选择文件
          <input type="file" multiple ref="fileInput" @change="handleFileSelect">
        </label>
      </div>
      
      <!-- 上传文件列表 -->
      <div class="upload-list" v-if="uploadFiles.length > 0">
        <h4 class="list-title">待上传文件 ({{ uploadFiles.length }})</h4>
        <div class="file-list">
          <div v-for="(file, index) in uploadFiles" :key="index" class="file-item">
            <div class="file-info">
              <div class="file-icon">
                <i class="fa" :class="getFileIcon(file.name)"></i>
              </div>
              <div class="file-details">
                <p class="file-name">{{ file.name }}</p>
                <p class="file-size">{{ formatFileSize(file.size) }}</p>
                <div v-if="file.status === 'uploading'" class="progress-bar">
                  <div class="progress" :style="{ width: file.progress + '%' }"></div>
                </div>
                <p v-if="file.error" class="error-text">{{ file.error }}</p>
              </div>
            </div>
            <el-button 
              icon="Close" 
              size="small" 
              circle 
              class="remove-btn"
              @click="removeFile(index)"
              :disabled="file.status === 'uploading'"
            ></el-button>
          </div>
        </div>
      </div>

      <!--  选择业务域 -->
      <div class="upload-settings upload-catalog">
        <label class="settings-label">业务域</label>
        <el-select size="large" v-model="selectedCatalogId" placeholder="请选择业务域" class="select-catalog"> 
          <el-option v-for="catalog in allCatalogs" :key="catalog.id" :value="catalog.id" :label="catalog.catalogName">
            {{ catalog.catalogName }}
          </el-option>
        </el-select>
      </div>
      
      <!-- 上传设置 -->
      <div class="upload-settings">
        <label class="settings-label">上传设置</label>
        <el-checkbox v-model="autoOverwrite" label="自动覆盖同名文件"></el-checkbox>
      </div>
    </div>
    
    <template #footer>
      <div class="footer-content">
        <div class="footer-left">
          <span v-if="isUploading">
            上传中: {{ completedCount }}/{{ uploadFiles.length }}
          </span>
        </div>
        <div class="footer-right">
          <el-button @click="closeUploadModal" class="default-btn">取消</el-button>
          <el-button 
            type="primary" 
            @click="startUpload" 
            class="primary-btn"
            :loading="isUploading"
            :disabled="uploadFiles.length === 0 || !selectedDirectory"
          >
            {{ isUploading ? '上传中...' : '开始上传' }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, defineProps, defineEmits, watch, onMounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  createFolder, 
  deleteFolder, 
  listFiles,
  catalogUploadFile as apiCatalogUploadFile
} from '@/api/data/lake/cloudFile';

import {
  listAllCatalog,
} from '@/api/data/lake/catalog';

const props = defineProps({
  showCatalogUploadModal: {
    type: Boolean,
    default: false
  },
  fileTypes: {
    type: Array,
    default: () => []
  },
  currentDirectory: {
    type: Object,
    default: () => ({ id: 0, name: '根目录', path: '/' })
  }
})

const emit = defineEmits(['update:showCatalogUploadModal', 'close', 'uploadSuccess'])

// 上传相关状态
const uploadPath = ref('')
const autoOverwrite = ref(false)
const shareAfterUpload = ref(false)
const isUploading = ref(false)
const completedCount = ref(0)
const selectedPathName = ref('')
const showTreeDropdown = ref(false)
const currentNodeKey = ref(0)
const selectedDirectory = ref(null)

const allCatalogs = ref([])
const selectedCatalogId = ref(null)

// 文件上传相关引用
const dropArea = ref(null)
const fileInput = ref(null)
const isDragging = ref(false)
const uploadFiles = ref([])
const treeRef = ref(null)
const loadingTree = ref(false)

// 文件夹树数据
const folderTree = ref([])

// 树形配置
const treeProps = ref({
  children: 'children',
  label: 'label'
})

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  isDirectory: 1
}) ;

// 默认展开的节点
const defaultExpandedKeys = ref([])

// 计算完整路径显示
const fullPath = computed(() => {
  if (!selectedDirectory.value) return '/';
  return selectedDirectory.value.path || '/';
});

// 计算显示的路径文本（如果太长则截断）
const fullPathDisplay = computed(() => {
  const path = fullPath.value;
  if (path.length <= 30) return path;
  
  // 如果路径太长，显示开头和结尾部分
  const start = path.substring(0, 15);
  const end = path.substring(path.length - 15);
  return `${start}...${end}`;
});

// 根据文件扩展名获取图标
const getFileIcon = (fileName) => {
  const extension = fileName.split('.').pop().toLowerCase()
  console.log('extension = ' + extension)

  // 通过后缀匹配文件类型
  const getFileType = (extension) => {
    return props.fileTypes.find(type => type.extension === extension);
  };
  
  return getFileType(extension)?.icon ; 
}

// 初始化上传路径
const initUploadPath = () => {
  if (props.currentDirectory) {
    selectedDirectory.value = props.currentDirectory
    selectedPathName.value = props.currentDirectory.name
    currentNodeKey.value = props.currentDirectory.id
    uploadPath.value = props.currentDirectory.path
  } else {
    // 默认值
    selectedDirectory.value = { id: 0, name: '根目录', path: '/' }
    selectedPathName.value = '根目录'
    currentNodeKey.value = 0
    uploadPath.value = '/'
  }
}

// 加载文件夹树
const loadFolderTree = async () => {
  try {
    loadingTree.value = true
    // 调用API获取文件夹树
    const response = await listFiles(queryParams.value)
    
    if (response && response.rows) {
      // 构建树形结构，这里需要根据实际API返回的数据结构调整
      folderTree.value = buildFolderTree(response.rows)
      defaultExpandedKeys.value = [currentNodeKey.value]
      
      // 设置当前选中节点
      nextTick(() => {
        if (treeRef.value) {
          treeRef.value.setCurrentKey(currentNodeKey.value)
        }
      })
    }
  } catch (error) {
    console.error('加载文件夹树失败:', error)
    ElMessage.error('加载文件夹树失败')
  } finally {
    loadingTree.value = false
  }
}

// 构建文件夹树形结构（根据实际API响应结构调整）
const buildFolderTree = (folders) => {
  // 实际实现应根据API返回的数据结构调整
  const map = {}
  const roots = []
  
  // 首先创建所有节点的映射
  folders.forEach(folder => {
    map[folder.id] = { ...folder, children: [] }
  })
  
  // 构建树形结构
  folders.forEach(folder => {
    if (folder.parentId === 0 || !map[folder.parentId]) {
      const node = {
        id: map[folder.id].id , 
        label: map[folder.id].fileName, 
      }
      // roots.push(map[folder.id])
      roots.push(node) ; 
    } else {
      const node = {
        id: map[folder.id].id , 
        label: map[folder.id].fileName, 
      }
      // map[folder.parentId].children.push(map[folder.id])
      map[folder.parentId].children.push(node) ; 
    }
  })
  
  return roots
}

// 关闭上传模态框
const closeUploadModal = () => {
  if (!isUploading.value) {
    resetUploadState()
    emit('update:showCatalogUploadModal', false)
    emit('close')
  } else {
    ElMessage.warning('文件正在上传中，请等待上传完成')
  }
}

// 重置上传状态
const resetUploadState = () => {
  uploadFiles.value = []
  isUploading.value = false
  completedCount.value = 0
  showTreeDropdown.value = false
  initUploadPath()
}

// 处理树节点点击
const handleNodeClick = (data) => {
  selectedDirectory.value = data
  selectedPathName.value = data.name
  uploadPath.value = data.path
  currentNodeKey.value = data.id
  showTreeDropdown.value = false
}

// 点击外部关闭下拉框
const handleClickOutside = (event) => {
  const pathSelector = document.querySelector('.path-selector-wrapper')
  if (pathSelector && !pathSelector.contains(event.target)) {
    showTreeDropdown.value = false
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 拖放事件处理
const handleDragEnter = () => {
  isDragging.value = true
  dropArea.value.classList.add('dragging')
}

const handleDragOver = () => {
  isDragging.value = true
}

const handleDragLeave = () => {
  isDragging.value = false
  dropArea.value.classList.remove('dragging')
}

const handleDrop = (e) => {
  isDragging.value = false
  dropArea.value.classList.remove('dragging')
  const files = e.dataTransfer.files
  if (files.length > 0) {
    handleFiles(files)
  }
}

// 处理文件选择
const handleFileSelect = () => {
  const files = fileInput.value.files
  if (files.length > 0) {
    handleFiles(files)
  }
  fileInput.value.value = ''
}

// 处理选中的文件
const handleFiles = (files) => {
  const newFiles = Array.from(files).map(file => ({
    file: file,
    name: file.name,
    size: file.size,
    progress: 0,
    status: 'waiting',
    error: null
  }))
  uploadFiles.value = [...uploadFiles.value, ...newFiles]
}

// 移除文件
const removeFile = (index) => {
  if (!isUploading.value) {
    uploadFiles.value.splice(index, 1)
  }
}

// 开始上传
const startUpload = async () => {
  if (uploadFiles.value.length === 0 || isUploading.value || !selectedDirectory.value) {
    ElMessage.warning('请选择要上传的文件和上传路径')
    return
  }
  
  isUploading.value = true
  completedCount.value = 0
  
  // 更新文件状态
  uploadFiles.value = uploadFiles.value.map(file => ({
    ...file,
    status: 'waiting',
    progress: 0,
    error: null
  }))
  
  // 依次上传每个文件
  for (let i = 0; i < uploadFiles.value.length; i++) {
    if (uploadFiles.value[i].status === 'waiting') {
      await uploadSingleFile(i)
    }
  }
  
  isUploading.value = false
  const successfulFiles = uploadFiles.value.filter(f => f.status === 'success')

  // 新增：只保留上传失败的文件
  uploadFiles.value = uploadFiles.value.filter(f => f.status === 'error')

  if (successfulFiles.length > 0) {
    ElMessage.success(`成功上传 ${successfulFiles.length} 个文件`)
    emit('uploadSuccess') // 通知父组件刷新文件列表
  }

  if (uploadFiles.value.length > 0) { // 修改判断条件，直接检查剩余失败文件数量
    ElMessage.warning(`有 ${uploadFiles.value.length} 个文件上传失败`)
  }else{
    // 关闭上传模态框
    closeUploadModal()
  }
}

// 上传单个文件
const uploadSingleFile = async (index) => {
  const fileData = uploadFiles.value[index]
  fileData.status = 'uploading'
  
  try {
    // 创建FormData对象
    const formData = new FormData()
    formData.append('file', fileData.file)
    formData.append('catalogId', selectedCatalogId.value)
    formData.append('parentId', selectedDirectory.value.id)
    formData.append('autoOverwrite', autoOverwrite.value)
    
    // 配置上传进度回调
    const config = {
      onUploadProgress: (progressEvent) => {
        if (progressEvent.lengthComputable) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          fileData.progress = percent
        }
      }
    }
    
    // 调用上传API
    const response = await apiCatalogUploadFile(formData, config)
    
    if (response.code === 200) {
      fileData.status = 'success'
      fileData.progress = 100
    } else {
      throw new Error(response.msg || '上传失败')
    }
  } catch (error) {
    console.error('文件上传失败:', error)
    fileData.status = 'error'
    fileData.error = error.message || '上传失败，请重试'
  } finally {
    completedCount.value++
  }
}

// 监听模态框显示状态
watch(() => props.showCatalogUploadModal, (newVal) => {
  if (newVal) {
    // 模态框打开时初始化
    resetUploadState()
    loadFolderTree()
  }
})

// 监听当前目录变化
watch(() => props.currentDirectory, (newVal) => {
  if (newVal) {
    initUploadPath()
  }
})

// 获取到所有的Catalog
const handleListAllCatalog = async () => {
  const response = await listAllCatalog()
  if (response.code === 200) {
    allCatalogs.value = response.data
  }
}

// 初始化
onMounted(() => {
  initUploadPath()
  handleListAllCatalog()
  document.addEventListener('click', handleClickOutside)
})

// 移除监听
import { onUnmounted } from 'vue'
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style lang="scss" scoped>
// 样式变量
$primary-color: #2c6ecb;
$primary-light: rgba(44, 110, 203, 0.1);
$border-color: #e5e6eb;
$text-primary: #1d2129;
$text-secondary: #4e5969;
$text-tertiary: #86909c;
$radius-md: 6px;
$radius-sm: 4px;
$error-color: #f56c6c;

.modal-body {
  padding: 0;
  max-height: 80vh;
  overflow-y: auto;
}

.drop-area {
  border: 2px dashed $border-color;
  border-radius: 8px;
  padding: 32px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 24px;

  &:hover, &.dragging {
    border-color: $primary-color;
    background-color: $primary-light;
  }

  .drop-icon {
    font-size: 48px;
    color: $primary-color;
    margin-bottom: 16px;
  }

  .drop-text {
    font-size: 16px;
    color: $text-primary;
    margin: 0 0 8px 0;
  }

  .drop-subtext {
    font-size: 12px;
    color: $text-tertiary;
    margin: 0 0 16px 0;
  }

  .select-file-btn {
    display: inline-block;
    background-color: $primary-color;
    color: #fff;
    border-radius: $radius-md;
    padding: 8px 16px;
    font-size: 14px;
    cursor: pointer;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: darken($primary-color, 5%);
    }

    input {
      display: none;
    }
  }
}

.upload-list {
  margin-bottom: 24px;
  
  .list-title {
    font-size: 14px;
    color: $text-primary;
    margin: 0 0 12px 0;
    font-weight: 500;
  }
  
  .file-list {
    max-height: 200px;
    overflow-y: auto;
    border: 1px solid $border-color;
    border-radius: $radius-md;
    padding: 8px;
  }
  
  .file-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px;
    border-radius: $radius-sm;
    margin-bottom: 4px;
    background: #fafafa;
    
    .file-info {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;
      min-width: 0;
      
      .file-icon {
        width: 32px;
        height: 32px;
        font-size: 22px;
        border-radius: $radius-sm;
        background: $primary-light;
        color: $primary-color;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
      
      .file-details {
        min-width: 0;
        flex: 1;
        
        .file-name {
          font-size: 14px;
          color: $text-primary;
          margin: 0 0 2px 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        .file-size {
          font-size: 12px;
          color: $text-tertiary;
          margin: 0 0 4px 0;
        }
        
        .progress-bar {
          height: 4px;
          background-color: #f0f0f0;
          border-radius: 2px;
          overflow: hidden;
          margin: 4px 0;
          
          .progress {
            height: 100%;
            background-color: $primary-color;
            transition: width 0.3s ease;
          }
        }
        
        .error-text {
          font-size: 12px;
          color: $error-color;
          margin: 4px 0 0 0;
        }
      }
    }
    
    .remove-btn {
      flex-shrink: 0;
      color: $text-tertiary;
      
      &:hover {
        color: #f5222d;
        background-color: rgba(245, 34, 45, 0.1);
      }
    }
  }
}

.upload-path {
  margin-bottom: 24px;

  .path-label {
    display: block;
    font-size: 14px;
    color: $text-primary;
    margin-bottom: 8px;
    font-weight: 500;
  }

  .path-selector-wrapper {
    position: relative;
    
    .path-display {
      display: flex;
      align-items: center;
      padding: 10px 12px;
      border: 1px solid $border-color;
      border-radius: $radius-md;
      background: #fff;
      cursor: pointer;
      transition: border-color 0.2s ease;
      
      &:hover {
        border-color: $primary-color;
      }
      
      .path-text-container {
        display: flex;
        flex-direction: column;
        flex: 1;
        min-width: 0;
        margin-right: 8px;
        
        .selected-path-text {
          font-size: 14px;
          color: $text-primary;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        .path-hint {
          font-size: 12px;
          color: $text-tertiary;
        }
      }
      
      .fa-chevron-down {
        transition: transform 0.2s ease;
        color: $text-tertiary;
      }
      
      .rotate-180 {
        transform: rotate(180deg);
      }
    }
    
    .tree-dropdown {
      position: absolute;
      top: 100%;
      left: 0;
      right: 0;
      background: #fff;
      border: 1px solid $border-color;
      border-radius: $radius-md;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      z-index: 1000;
      margin-top: 4px;
      max-height: 200px;
      overflow-y: auto;
      
      .loading-tree {
        padding: 16px;
        text-align: center;
        color: $text-tertiary;
      }
    }
  }
}

.upload-settings {
  margin-bottom: 24px;

  .settings-label {
    display: block;
    font-size: 14px;
    color: $text-primary;
    margin-bottom: 8px;
    font-weight: 500;
  }

  .el-checkbox {
    margin-right: 16px;
    margin-bottom: 8px;
  }
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  
  .footer-left {
    font-size: 14px;
    color: $text-secondary;
  }
  
  .footer-right {
    display: flex;
    gap: 12px;
  }
}

.tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.select-catalog{
  width:100%;
}

// 树形组件样式
.folder-tree {
  padding: 8px;
  
  .el-tree-node {
    .el-tree-node__content {
      height: 36px;
      padding: 4px 0;
      
      &:hover {
        background-color: rgba(44, 110, 203, 0.1);
      }
    }
    
    &.is-current > .el-tree-node__content {
      background-color: rgba(44, 110, 203, 0.1);
      color: #2c6ecb;
      font-weight: 500;
      
      .fa-folder {
        color: #2c6ecb !important;
      }
    }
  }
  
  .el-tree-node__expand-icon {
    padding: 8px;
  }
}
</style>