<template>
  <div class="table-metadata-container">
    <h3>表元数据</h3>
    <el-table 
      :data="fields" 
      stripe 
      style="width: 100%"
      :header-cell-style="{ 'background-color': '#f5f7fa', 'font-weight': 'bold' }"
    >

      <!-- 序号 -->
       <el-table-column 
        type="index" 
        label="序号" 
        align="center"
        width="50"
      ></el-table-column>

      <!-- 字段名称列 -->
      <el-table-column 
        prop="name" 
        label="字段名称" 
        align="left"
        width="300"
      ></el-table-column>
      
      <!-- 数据类型列 -->
      <el-table-column 
        label="数据类型" 
        align="left"
        width="150"
      >
        <template #default="scope">
          <span>
            {{ scope.row.dataType }}
            <template v-if="scope.row.length">
              ({{ scope.row.length }})
            </template>
          </span>
        </template>
      </el-table-column>
      
      <!-- 主键列 -->
      <el-table-column 
        prop="isPrimary" 
        label="主键" 
        align="center"
        width="80"
      >
        <template #default="scope">
          <el-icon v-if="scope.row.isPrimary" color="#1677ff">
            <Check />
          </el-icon>
          <el-icon v-else color="#ccc">
            <Close />
          </el-icon>
        </template>
      </el-table-column>
      
      <!-- 非空列 -->
      <el-table-column 
        prop="notNull" 
        label="非空" 
        align="center"
        width="80"
      >
        <template #default="scope">
          <el-icon v-if="scope.row.notNull" color="#1677ff">
            <Check />
          </el-icon>
          <el-icon v-else color="#ccc">
            <Close />
          </el-icon>
        </template>
      </el-table-column>
      
      <!-- 分区列 -->
      <el-table-column 
        prop="isPartition" 
        label="分区字段" 
        align="center"
        width="100"
      >
        <template #default="scope">
          <el-icon v-if="scope.row.isPartition" color="#1677ff">
            <Check />
          </el-icon>
          <el-icon v-else color="#ccc">
            <Close />
          </el-icon>
        </template>
      </el-table-column>
      
      <!-- 描述列 -->
      <el-table-column 
        prop="description" 
        label="描述" 
        align="left"
      ></el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { defineProps } from 'vue';
import { Check, Close } from '@element-plus/icons-vue';

// 定义从父组件接收的props
const props = defineProps({
  fields: {
    type: Array,
    required: true,
    default: () => []
  }
});
</script>

<style scoped lang="scss">
.table-metadata-container {
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  
  h3 {
    margin: 0 0 16px 0;
    color: #1f2329;
    font-size: 16px;
    font-weight: 500;
  }
}
</style>
