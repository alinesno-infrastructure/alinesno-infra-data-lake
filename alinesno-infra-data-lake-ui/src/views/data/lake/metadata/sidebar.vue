<template>
  <div class="app-container sidebar-meta-container">
    <div class="head-action-btn">
      <span>
        域目录 
      </span>
      <span>
        <el-button type="primary" @click="expandTree" text bg> 
          <i class="fa-solid fa-up-down"></i> &nbsp; {{ isTreeExpanded?'收起':'展开' }}
         </el-button>
      </span> 
    </div>
    <div class="head-container">
      <el-input v-model="deptName" placeholder="请输入目录名称" clearable prefix-icon="Search"
        style="margin-bottom: 20px" />
    </div>
    <div class="head-container">
      <el-scrollbar style="height: calc(100vh - 200px)">
        <el-tree 
          :data="deptOptions" 
          :props="{ label: 'label', children: 'children' }" 
          :expand-on-click-node="false"
          :default-expand-all="false"
          :filter-node-method="filterNode" 
          ref="deptTreeRef" 
          node-key="id" 
          highlight-current
          @node-click="handleNodeClick" 
          v-loading="loading"
        >
          <template #default="{ node, data }">
            <div class="custom-tree-node" :class="(selectNodeItem && selectNodeItem.id === node.id)?'active':''" style="height:auto;">
                <div style="display: flex;flex-direction: column;">
                    <div style="font-size: 14px;">
                        <i v-if="data.isDirectory" class="fa-solid fa-folder"></i>
                        <i v-else class="fa-solid fa-table-cells-large"></i>
                        {{ node.label }} 
                    </div>
                    <div class="description">
                        <span style="color: #999;">{{ data.desc }}</span>
                    </div>
                </div>
            </div>
          </template>
          <template #empty>
            <div v-if="!loading">没有找到匹配的目录</div>
          </template>
        </el-tree>
      </el-scrollbar>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue';
import { 
  getCatalogTree 
} from '@/api/data/lake/catalogTable';

// 定义emit，用于向父组件传递事件
const emit = defineEmits(['node-click', 'update:selectNode']);

// 数据湖目录结构
const deptOptions = ref([]);
const deptName = ref('');
const deptTreeRef = ref(null);
const loading = ref(false);
const selectNodeItem = ref(null);
const isTreeExpanded = ref(false);

const route = useRoute();
const catalogId = ref(route.query.catalogId); 

// 加载目录数据
const loadCatalogData = async () => {
  try {
    loading.value = true;
    const response = await getCatalogTree(catalogId.value);
    deptOptions.value = response.data ;

    handleNodeClick(response.data[0])

    nextTick(() => {
      isTreeExpanded.value = false;
      expandTree();
    });

  } catch (error) {
    console.error('加载目录数据失败:', error);
    deptOptions.value = [];
  } finally {
    loading.value = false;
  }
};

// 节点过滤方法
const filterNode = (value, data) => {
  if (!value) return true;
  return data.label.toLowerCase().includes(value.toLowerCase());
};

// 节点点击事件处理
const handleNodeClick = (data) => {
  console.log('点击了节点:', data);
  selectNodeItem.value = data;  // 更新选中节点
  emit('node-click', data);     // 向父组件发送节点点击事件
  emit('update:selectNode', data); // 更新选中节点
};

// 目录收缩与打开 
const expandTree = () => {
    if (!deptTreeRef.value) return;
    
    // 获取所有节点
    const nodes = deptTreeRef.value.store.nodesMap;
    
    // 切换所有节点的展开状态
    Object.values(nodes).forEach(node => {
        node.expanded = !isTreeExpanded.value;
    });
    
    // 更新状态
    isTreeExpanded.value = !isTreeExpanded.value; 
};

// 监听搜索框变化，实现过滤功能
watch(deptName, (value) => {
  deptTreeRef.value?.filter(value);
});

// 组件挂载时加载数据
onMounted(() => {
  loadCatalogData();
});

// 对父类暴露方法
// 使用 defineExpose 暴露 show 方法
defineExpose({
  loadCatalogData
})

</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
}

.sidebar-meta-container {
  background-color: #fff !important;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.head-container {
  width: 100%; 
}
:deep(.el-tree-node__content) {
    height: auto !important;
} 

.head-action-btn {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  border-radius: 8px;
  padding: 5px;
  margin-bottom: 10px;
  background: #fff;
  color: #777;
}

.custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    padding-right: 8px;

    &.active {
        border-radius: 8px;
        background: #f5f7fa;
    }

    .description {
        margin-top: 0px;
        width: 100%;
        padding: 5px 0;
        padding-right: 20px;
        white-space: pre-wrap;
        color:#a5a5a5 !important;
        /* 使文本自动换行 */
    }

    .func-drapdown {
        padding: 10px;
        margin-right: 20px;
    }

}
</style>
