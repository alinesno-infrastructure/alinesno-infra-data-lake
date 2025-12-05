<template>
  <div class="data-catalog">
    <!-- 顶部区域 -->
    <header class="header">
      <div class="path-nav">
        <button class="back-btn" @click="handleBack">
          <div class="catalog-panel-icon" v-if="currentCatalog?.catalogIcon">
            <img :src="imagePath(currentCatalog.catalogIcon)" />
           </div>
        </button>
        <span class="path-text"> {{ currentCatalog?.catalogName }} 

          <el-button type="primary" text bg @click="handleCopy(currentCatalog?.id)" >
              标识:{{ currentCatalog?.id }} &nbsp; <i class="fa-solid fa-copy"></i>
          </el-button>

         </span>
      </div>

      <span style="display: flex;gap: 10px;">
        <el-button @click="showCreateModal = true" size="default" type="primary">
          <i class="fas fa-plus"></i>
          新建目录
        </el-button>
        <CreateTableBtn 
          @handleRefreshSidebar="handleRefreshSidebar"
          :currentCatalogTable="currentCatalogTable" />

        <!-- 同步 -->
        <el-button @click="handleSyncTableStructure()" size="default" type="success">
          <i class="fa-solid fa-rotate"></i>
          同步表结构
        </el-button>
      </span>

    </header>

    <!-- 动态导航栏 -->
    <div class="path-nav-breadcrumb">
      <el-breadcrumb :separator-icon="ArrowRight">
        <el-breadcrumb-item class="nav-item" @click="handleBreadcrumbClick(null)">根目录</el-breadcrumb-item>
        <el-breadcrumb-item 
          class="nav-item" 
          v-for="(item, index) in breadcrumbItems" 
          :key="index"
          @click="handleBreadcrumbClick(item)"
        >
          {{ item.label }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    
    <!-- 标签栏 -->
    <div class="tabs">
      <button 
        class="tab" 
        :class="{ active: activeTab === 'details' }"
        @click="activeTab = 'details'"
      >
        详细信息
      </button>
      <button 
        class="tab" 
        :class="{ active: activeTab === 'storage' }"
        @click="activeTab = 'storage'"
      >
        存储概览
      </button>
      <button 
        class="tab" 
        :class="{ active: activeTab === 'resources' }"
        @click="activeTab = 'resources'"
      >
        资源概览
      </button>

      <!--
      <button 
        class="tab" 
        :class="{ active: activeTab === 'permissions' }"
        @click="activeTab = 'permissions'"
      >
        权限
      </button>
      -->

    </div>
    
    <!-- 内容区域 -->
    <el-scrollbar style="height:calc(100vh - 270px)" >
      <div class="content">
        <!-- 基本信息板块 -->
        <section class="info-section" v-if="activeTab === 'details'">
          <h2 class="section-title">基本信息</h2>
          <div class="info-grid">

            <div class="info-item" v-for="(item, index) in catalogInfo" :key="index">
              <div class="info-label">{{ item.label }}</div>
              <div class="info-value">
                <!-- 对“描述”显示可编辑输入 -->
                <template v-if="item.code === 'description'">
                  <template v-if="!editingDescription">
                    <span class="info-value-text">{{ item.value || '-' }}</span>
                    <el-button type="primary" text bg size="mini" @click="handleEditDescription" style="margin-left:8px;">
                      <i class="fa-solid fa-pen-to-square"></i>
                    </el-button>
                  </template>

                  <template v-else>
                    <!-- 使用 element-plus 的输入组件或普通 textarea -->
                    <el-input
                      v-model="descriptionDraft"
                      type="textarea"
                      :rows="2"
                      resize="none"
                      placeholder="请输入描述"
                      style="max-width:480px;"
                    />
                    <el-button @click="handleCancelEditDescription" size="mini" style="margin-left:8px;">取消</el-button>
                    <el-button type="primary" @click="handleSaveDescription" size="mini" style="margin-left:6px;">保存</el-button>
                  </template>
                </template>

                <!-- 其它字段保留原来渲染 -->
                <template v-else>
                  <span class="info-value-text">
                    {{ item.value }}
                  </span>
                  <el-button v-if="item.copy == true" type="info" text bg @click="handleCopy(item.copyText?item.copyText:item.value)">
                    <i class="fa-solid fa-copy"></i>
                  </el-button>
                </template>
              </div>
            </div>

          </div>
        </section>
        
        <!-- 其他标签内容区域 -->
        <!-- <section class="other-section" v-else>
          <h2 class="section-title">{{ tabTitles[activeTab] }}</h2>
          <div class="empty-state">
            此标签页的内容将在这里显示
          </div>
        </section> -->

        <!-- 显示Fieldg列表-->
        <TableMetadata v-if="currentCatalogTableFields && activeTab === 'details'" :fields="currentCatalogTableFields" />
      </div>

      <!-- 存储概览 -->
      <StorageOverview class="storage-section" v-if="activeTab === 'storage'" />

      <!-- 资源概览 -->
      <ResourceOverview class="resource-section" v-if="activeTab === 'resources'" />

      <!-- 权限信息 
      <PowerInfo class="permission-section" v-if="activeTab === 'permissions'" /> 
       -->

    </el-scrollbar>

    
    <!-- 新建表/目录对话框 (使用el-dialog) -->
    <el-dialog
      :title="'新建' + (formData.isDirectory ? '目录' : '表')"
      v-model="showCreateModal"
      :width="'700px'"
      :close-on-click-modal="false"
      :destroy-on-close="true"
      size="large"
      @close="resetForm"
    >
      <div class="modal-body">
        <form class="create-form">
          <div class="form-group">
            <label>名称 <span class="required">*</span></label>
            <input 
              type="text" 
              v-model="formData.tableName"
              placeholder="请输入名称"
              required
            >
          </div>
          
          <div class="form-group">
            <label>类型 <span class="required">*</span></label>
            <div class="radio-group">
              <label class="radio-item">
                <input 
                  type="radio" 
                  :value="true" 
                  v-model="formData.isDirectory"
                >
                目录
              </label>
            </div>
          </div>
          
          <div class="form-group" v-if="!formData.isDirectory">
            <label>格式类型</label>
            <select v-model="formData.formatType">
              <option value="">请选择格式</option>
              <option value="PARQUET">Parquet</option>
              <option value="ORC">ORC</option>
              <option value="CSV">CSV</option>
              <option value="JSON">JSON</option>
              <option value="METADATA">元数据</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>父节点</label>
            <div class="tree-selector">
              <el-tree
                :data="catalogTree"
                :props="treeProps"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="tree"
                node-key="id"
                @node-click="handleNodeClick"
              >
              <template #default="{ node, data }">
                {{ node.label + ' (' + data.desc + ')' }}
              </template>
            </el-tree>
              <!-- <input
                type="text"
                placeholder="搜索节点..."
                v-model="filterText"
                @input="filterTree"
              > -->
            </div>
            <p class="help-text">当前选择: {{ selectedNode?.label || '根节点' }}</p>
          </div>
          
          <div class="form-group">
            <label>描述</label>
            <textarea 
              v-model="formData.description"
              placeholder="请输入描述信息"
              rows="3"
            ></textarea>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <el-button @click="showCreateModal = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleCreate" size="large">确认创建</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElDialog, ElButton, ElMessage , ElTree } from 'element-plus';

import {
  getCatalog,
} from '@/api/data/lake/catalog';

import {
  createCatalogDirectory,
  getCatalogTree,
  syncTableStructure,
  findById,
  updateCatalogTable
} from '@/api/data/lake/catalogTable';

import CreateTableBtn from "./createTable.vue"
import TableMetadata from './tableField.vue';

// 组件
import StorageOverview from './tabs/storageOverview.vue';
import ResourceOverview from './tabs/resourceOverview.vue';
import PowerInfo from './tabs/powerInfo.vue';

const emit = defineEmits(['refreshSidebar']);

const route = useRoute();
const catalogId = ref(route.query.catalogId); 

// 接收父组件传递的当前节点
const props = defineProps({
  currentNode: {
    type: Object,
    default: null
  }
});

// 导航路径数组
const breadcrumbItems = ref([]);
// 节点详情数据
const nodeDetails = ref(null);
// 活跃标签
const activeTab = ref('details');

// 当前目录信息
const currentCatalog = ref(null);

// 当前目录表
const currentCatalogTable = ref(null);

// 当前目录表字段
const currentCatalogTableFields = ref(null);

// 搜索数据库名称
const searchDbName = ref('');

// 标签页标题
const tabTitles = {
  details: '详细信息',
  permissions: '权限设置',
  storage: '存储概览',
  resources: '资源概览'
};

// 目录基本信息
// 改为计算属性，基于currentCatalogTable动态生成
const catalogInfo = computed(() => {
  if (!currentCatalogTable.value) {
    return [];
  }
  
  const data = currentCatalogTable.value;
  return [
    { label:  `${data.isDirectory?'目录':'表'}名称`, value: data.tableName || '-' , copy: data.isDirectory?true:true , copyText: data.id},
    { label: '描述', value: data.description || '-' , code: 'description' },
    { label: '创建时间', value: data.addTime || '-' },
    { label: '存储分层配置', value: 'ns' + data.schemeName , copy: true }, // 假设默认配置，可根据实际数据调整
    // { label: '类型', value: data.formatType || '-' }, // 假设Hive类型，可根据实际数据调整
    // { label: '存储大小', value: `${data.statistics?.formattedSize || 0}` },
    // { label: `${data.isDirectory?'表':'存储'}数量`, value: (data.statistics?.fileCount || 0) },
    { label: '存储位置', value: data.storageLocation || '-'  , copy: true},
  ];
});

// 新建表/目录相关
const showCreateModal = ref(false);
const catalogTree = ref([]);
const filterText = ref('');
const selectedNode = ref(null);
const treeProps = {
  label: 'label',
  children: 'children',
  isLeaf: 'isLeaf'
};

// 表单数据
const formData = ref({
  tableName: '',
  description: '',
  isDirectory: true,
  formatType: '',
  parentId: 0,
  catalogId: 0
});

const formType = ref('directory');


// 加载节点详情
const loadNodeDetails = async (node) => {
  if (!node) return;
  
  // 保存当前节点信息
  nodeDetails.value = node;
  
  // 更新导航路径
  updateBreadcrumb(node);

  const response = await findById(node.id); // 假设目录ID可以直接用于获取详情
  currentCatalogTable.value = response.data;
  currentCatalogTable.value.statistics = response.statistics ;
  currentCatalogTableFields.value = response.fields;
};

// 更新导航路径
const updateBreadcrumb = (node) => {
  // 清空现有路径
  breadcrumbItems.value = [];
  
  // 递归获取所有父节点构建完整路径
  const buildPath = async (currentNode) => {
    if (!currentNode) return;
    
    // 将当前节点添加到路径数组开头
    breadcrumbItems.value.unshift({
      id: currentNode.id,
      label: currentNode.label,
      isDirectory: currentNode.isDirectory
    });
    
    // 如果有父节点，继续向上查找
    if (currentNode.parentId && currentNode.parentId !== 0) {
      // 实际应用中应该调用API获取父节点信息
      // 这里简化处理，从目录树中查找
      const parentNode = findNodeInTree(catalogTree.value, currentNode.parentId);
      if (parentNode) {
        await buildPath(parentNode);
      }
    }
  };
  
  buildPath(node);
};

// 从目录树中查找节点
const findNodeInTree = (tree, nodeId) => {
  for (const node of tree) {
    if (node.id === nodeId) {
      return node;
    }
    if (node.children && node.children.length > 0) {
      const found = findNodeInTree(node.children, nodeId);
      if (found) return found;
    }
  }
  return null;
};

// 处理导航项点击
const handleBreadcrumbClick = (item) => {
  if (!item) {
    // 点击了根节点(global)，需要处理
    nodeDetails.value = null;
    breadcrumbItems.value = [];
    // 可以添加加载根节点数据的逻辑
  } else {
    // 加载点击的导航项对应的节点详情
    loadNodeDetails(item);
  }
};

// 监听currentNode变化，更新详情
// watch(() => props.currentNode, (newNode) => {
//   if (newNode) {
//     loadNodeDetails(newNode);
//   }
// });

// 获取目录树
const getCatalogTreeData = () => {
  getCatalogTree(catalogId.value).then(res => {
    // 定义递归过滤函数
    const filterDirectories = (nodes) => {
      // 先过滤当前层级的非目录节点
      return nodes
        .filter(node => node.isDirectory === true)
        .map(node => {
          // 如果有子节点，递归过滤子节点
          if (node.children && node.children.length > 0) {
            return {
              ...node,
              children: filterDirectories(node.children)
            };
          }
          return node;
        });
    };

    // 应用过滤
    catalogTree.value = filterDirectories(res.data);
    console.log("过滤后的目录树:", catalogTree.value);
  }).catch(err => {
    console.error("获取目录树失败:", err);
  });
};


// 过滤树节点
const filterNode = (value, data) => {
  if (!value) return true;
  return data.label.toLowerCase().includes(value.toLowerCase());
};

// 处理复制功能
const handleCopy = (item) => {
  // 检查是否有可复制的值
  if (!item) {
    ElMessage.warning('没有可复制的内容');
    return;
  }

  // 使用 Clipboard API 复制文本
  navigator.clipboard.writeText(item)
    .then(() => {
      // 复制成功提示
      ElMessage.success('复制成功');
    })
    .catch((err) => {
      // 复制失败处理（兼容旧浏览器）
      console.error('复制失败:', err);
      // 降级方案：使用文本框临时存储并复制
      const textarea = document.createElement('textarea');
      textarea.value = item;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      ElMessage.success('复制成功');
    });
};


const filterTree = () => {
  const treeRef = document.querySelector('.el-tree');
  if (treeRef && treeRef.__vue__) {
    treeRef.__vue__.$refs.tree.filter(filterText.value);
  }
};

// 处理节点点击
const handleNodeClick = (data) => {
  selectedNode.value = data;
  formData.value.parentId = data.id;
};

// 处理返回
const handleBack = () => {
  console.log('返回上一级');
  // 实际应用中可以使用路由返回
};

// 处理搜索数据库
const handleSearchDB = () => {
  console.log('搜索数据库:', searchDbName.value);
};

// 处理编辑数据库
const handleEditDB = (db) => {
  console.log('编辑数据库:', db);
  // 实际应用中可以打开编辑数据库的模态框
};

// 处理删除数据库
const handleDeleteDB = (db, index) => {
  console.log('删除数据库:', db);
  if (confirm(`确定要删除数据库 ${db.name} 吗？`)) {
    databases.value.splice(index, 1);
  }
};

// 处理创建
const handleCreate = () => {
  if (!formData.value.tableName) {
    // 使用Element的Message组件显示错误提示
    ElMessage.error('请输入名称');
    return;
  }
  
  // 补充目录ID
  formData.value.catalogId = catalogId.value;
  
    createCatalogDirectory(formData.value).then(res => {
    if (res.code == 200) {
      ElMessage.success('创建成功');
      showCreateModal.value = false;
      // 重置表单
      resetForm();
      // 刷新目录树
      getCatalogTreeData();
      // 这里可以根据需要刷新数据库列表
      emit('refreshSidebar')
    } else {
      ElMessage.error('创建失败: ' + (res.message || '未知错误'));
    }
  }).catch(err => {
    console.error('创建失败', err);
    ElMessage.error('创建失败，请稍后重试');
  });
};

// 处理更新
const handleRefreshSidebar = () => { 
  emit('refreshSidebar')
};

// 重置表单
const resetForm = () => {
  formData.value = {
    tableName: '',
    description: '',
    isDirectory: true,
    formatType: '',
    parentId: 0,
    catalogId: catalogId.value
  };
  formType.value = 'directory';
  selectedNode.value = null;
  filterText.value = '';
};

const handleSyncTableStructure = () => {
  syncTableStructure(catalogId.value).then(res => {
    ElMessage.success('同步成功');
    emit('refreshSidebar')
  })
}

// 获取目录信息
const handleGetCatalog = () => { 
  getCatalog(catalogId.value).then(res => {
    console.log(res);
    currentCatalog.value = res.data;
  });
};

// 描述编辑状态
const editingDescription = ref(false);
const descriptionDraft = ref('');

// 当当前表加载或变化时同步初始描述内容
watch(currentCatalogTable, (newVal) => {
  if (newVal) {
    descriptionDraft.value = newVal.description || '';
  } else {
    descriptionDraft.value = '';
  }
});

// 进入编辑
const handleEditDescription = () => {
  if (!currentCatalogTable.value) {
    ElMessage.warning('当前没有选中表/目录');
    return;
  }
  descriptionDraft.value = currentCatalogTable.value.description || '';
  editingDescription.value = true;
};

// 取消编辑
const handleCancelEditDescription = () => {
  descriptionDraft.value = currentCatalogTable.value?.description || '';
  editingDescription.value = false;
};

// 保存描述（调用后端更新接口）
const handleSaveDescription = async () => {
  if (!currentCatalogTable.value) {
    ElMessage.warning('当前没有选中表/目录');
    return;
  }

  // 如果没有改动，直接关闭
  if ((currentCatalogTable.value.description || '') === (descriptionDraft.value || '')) {
    editingDescription.value = false;
    ElMessage.info('描述未改变');
    return;
  }

  try {
    const payload = {
      id: currentCatalogTable.value.id,
      description: descriptionDraft.value
    };
    const res = await updateCatalogTable(payload);
    if (res && (res.code === 200 || res.success === true)) {
      // 更新本地数据
      currentCatalogTable.value.description = descriptionDraft.value;
      ElMessage.success('保存成功');
      editingDescription.value = false;
      emit('refreshSidebar')
    } else {
      ElMessage.error('保存失败: ' + (res.message || '未知错误'));
    }
  } catch (err) {
    console.error('保存描述失败', err);
    ElMessage.error('保存失败，请稍后重试');
  }
};

// 监听模态框显示状态
watch(showCreateModal, (newVal) => {
  if (newVal) {
    getCatalogTreeData();
    resetForm();
  }
});

onMounted(() => {
  handleGetCatalog();
  getCatalogTreeData();
  formData.value.catalogId = catalogId.value;
});

defineExpose({
  loadNodeDetails
})

</script>

<style lang="scss" scoped>
.data-catalog {
  min-height: calc(100vh - 80px);
  color: #333;
  line-height: 1.6;
  padding: 16px;
}

// 顶部区域样式
.header {
  padding: 8px 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 5px;
  background: #fafafa;
  margin-bottom: 6px;
  border-bottom: 0px solid #eee;

  .path-nav {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .back-btn {
    background: none;
    border: none;
    color: #666;
    cursor: pointer;
    font-size: 18px;
    padding: 4px;
    border-radius: 4px;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f0f0f0;
    }
  }

  .path-text {
    color: #333;
    font-size: 16px;
    font-weight: 500;
  }

  .new-db-btn {
    background-color: #007bff;
    color: white;
    border: none;
    padding: 8px 16px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: background-color 0.2s;

    &:hover {
      background-color: #0056b3;
    }
  }
}

// 标签栏样式
.tabs {
  background-color: #fff;
  display: flex;
  overflow-x: auto;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border-radius: 8px;

  .tab {
    padding: 10px 24px;
    border: none;
    background: none;
    font-size: 14px;
    font-weight: 500;
    color: #666;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    transition: all 0.2s;

    &.active {
      color: #007bff;
      border-bottom-color: #007bff;
    }

    &:hover:not(.active) {
      color: #333;
      background-color: #f5f5f5;
    }
  }
}

// 内容区域样式
.content {
  padding: 0 10px;
  margin: 0 auto;
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

// 基本信息板块
.info-section {
  border-radius: 8px;
  padding: 0px;
  margin-bottom: 24px;
  // box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  .info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
    gap: 20px;
  }

  .info-item {
    margin-bottom: 8px;

    .info-label {
      font-size: 13px;
      color: #666;
      margin-bottom: 4px;
    }

    .info-value {
      font-size: 14px;
      color: #333;
      font-weight: 500;
      display: flex;
      align-items: center;

      .info-value-text{
        white-space: nowrap; /* 禁止换行 */
        overflow: hidden; /* 超出部分隐藏 */
        text-overflow: ellipsis; /* 超出部分显示省略号 */
      }
    }
  }
}

// 数据库板块
.db-section {
  background-color: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  .db-controls {
    margin-bottom: 16px;

    .db-input {
      width: 100%;
      max-width: 400px;
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
      transition: border-color 0.2s;

      &:focus {
        outline: none;
        border-color: #007bff;
      }
    }
  }

  .db-table {
    width: 100%;
    border-collapse: collapse;

    th, td {
      padding: 12px 16px;
      text-align: left;
      font-size: 14px;
      border-bottom: 1px solid #f0f0f0;
    }

    th {
      color: #666;
      font-weight: 500;
      background-color: #f9fafb;
    }

    tr:hover {
      background-color: #f9fafb;
    }
  }

  .action-btn {
    background: none;
    border: none;
    color: #666;
    cursor: pointer;
    font-size: 14px;
    padding: 4px 8px;
    border-radius: 4px;
    transition: all 0.2s;
    margin-right: 4px;

    &.edit:hover {
      color: #007bff;
      background-color: rgba(0, 123, 255, 0.1);
    }

    &.delete:hover {
      color: #dc3545;
      background-color: rgba(220, 53, 69, 0.1);
    }
  }
}

// 其他标签内容样式
.other-section {
  background-color: white;
  border-radius: 8px;
  padding: 0px;
  min-height: 300px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  .empty-state {
    color: #666;
    text-align: center;
    padding: 40px 0;
    border: 1px dashed #e0e0e0;
    border-radius: 4px;
    margin-top: 20px;
  }
}

// 对话框表单样式
.modal-body {
  padding: 10px 0;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;

  &:focus {
    outline: none;
    border-color: #007bff;
  }
}

.radio-group {
  display: flex;
  gap: 16px;
  padding: 8px 0;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.tree-selector {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tree-selector .el-tree {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 8px;
  height: 200px;
  overflow-y: auto;
}

.help-text {
  margin: 0;
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.required {
  color: #dc3545;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.path-nav-breadcrumb {
  background-color: #fafafa;
  padding: 8px;
  margin-bottom: 8px;
  border-radius: 8px;

  .nav-item {
    font-size: 15px !important;
  }
}

// 响应式样式
@media (max-width: 768px) {
  .info-section {
    .info-grid {
      grid-template-columns: 1fr;
    }
  }

  .db-section {
    .db-table {
      th:nth-child(3),
      td:nth-child(3),
      th:nth-child(4),
      td:nth-child(4),
      th:nth-child(5),
      td:nth-child(5) {
        display: none;
      }
    }
  }
}

.catalog-panel-icon{
  width: 30px;
  height: 30px;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
  }
}
</style>
