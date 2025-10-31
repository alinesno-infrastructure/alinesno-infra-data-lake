<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 类型数据 -->
      <el-col :span="6" :xs="24">
        <!-- 添加node-click事件监听 -->
        <MetaSidebarPanel 
          ref="metaSidebarPanelRef" 
          @node-click="handleSidebarNodeClick"
        />
      </el-col>

      <!-- 元数据信息 -->
      <el-col :span="18" :xs="20">
        <MetaMainPanel 
          @refreshSidebar="refreshSidebar"
          ref="metaMainPanelRef" 
          :current-node="currentNode"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>

import MetaSidebarPanel from "./sidebar"
import MetaMainPanel from "./metaPanel"
import { ref } from 'vue';

const metaSidebarPanelRef = ref(null) 
const metaMainPanelRef = ref(null) 
const currentNode = ref(null);  // 存储当前选中的节点

// 处理侧边栏节点点击事件
const handleSidebarNodeClick = (node) => {
  currentNode.value = node;  // 更新当前节点
  // 通知metaMainPanel刷新数据
  if (metaMainPanelRef.value) {
    metaMainPanelRef.value.loadNodeDetails(node);
  }
};

const refreshSidebar = () => {
  metaSidebarPanelRef.value.loadCatalogData();
};
</script>

<style lang="scss" scoped>
.app-container {
  background-color: #fff;
  padding: 10px;
}
</style>
