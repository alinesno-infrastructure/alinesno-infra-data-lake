<!-- 修改后的模板部分 -->
<template>
  <div>
    <el-button @click="handleShowCreateModal" type="warning" size="default">
      <i class="fas fa-plus"></i>
      新建表
    </el-button>

    <!-- 表添加对话框 -->
    <el-dialog
      v-model="showCreateModal"
      title="新建数据表"
      width="80%"
      style="max-width:1300px"
      :before-close="handleClose"
    >
      <el-form ref="tableFormRef" :model="tableForm" label-width="80px" size="default">
        <!-- 基本信息部分 -->
        <el-form-item label="表名称" prop="tableName" required>
          <el-input 
            v-model="tableForm.tableName" 
            placeholder="请输入表名（以字母开头，1-64位，支持字母、数字和下划线）"
            maxlength="64"
          ></el-input>
          <span v-if="tableNameError" class="error-text">{{ tableNameError }}</span>
        </el-form-item>

        <el-form-item label="格式" prop="format">
          <el-select v-model="tableForm.format" placeholder="请选择格式">
            <el-option label="CSV" value="csv"></el-option>
            <el-option label="JSON" value="json"></el-option>
            <el-option label="Parquet" value="parquet"></el-option>
            <el-option label="ORC" value="orc"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="表描述" prop="description">
          <el-input
            v-model="tableForm.description"
            placeholder="请输入表描述信息"
          ></el-input>
        </el-form-item>

        <!-- 分割线 -->
        <el-divider content-position="left">表字段信息</el-divider>

        <!-- 字段信息表格 -->
        <div class="fields-table-container">
          <el-table
            :data="tableForm.fields"
            border
            style="width: 100%; margin-bottom: 16px"
          >
            <el-table-column label="列名称" prop="name" width="120">
              <template #default="scope">
                <el-input v-model="scope.row.name" placeholder="列名"></el-input>
              </template>
            </el-table-column>
            
            <el-table-column label="主键" prop="isPrimary" align="center" width="80">
              <template #default="scope">
                <el-checkbox v-model="scope.row.isPrimary"></el-checkbox>
              </template>
            </el-table-column>
            
            <el-table-column label="非空" prop="notNull" width="80">
              <template #default="scope">
                <el-checkbox v-model="scope.row.notNull"></el-checkbox>
              </template>
            </el-table-column>
            
            <el-table-column label="分区字段" prop="isPartition" width="100">
              <template #default="scope">
                <el-checkbox v-model="scope.row.isPartition"></el-checkbox>
              </template>
            </el-table-column>
            
            <el-table-column label="数据类型" prop="dataType" width="120">
              <template #default="scope">
                <el-select v-model="scope.row.dataType" placeholder="选择类型">
                  <el-option label="INT" value="int"></el-option>
                  <el-option label="BIGINT" value="bigint"></el-option>
                  <el-option label="VARCHAR" value="varchar"></el-option>
                  <el-option label="DATE" value="date"></el-option>
                  <el-option label="TIMESTAMP" value="timestamp"></el-option>
                  <el-option label="BOOLEAN" value="boolean"></el-option>
                  <el-option label="DOUBLE" value="double"></el-option>
                </el-select>
              </template>
            </el-table-column>
            
            <el-table-column label="长度/类型" prop="length" width="120">
              <template #default="scope">
                <el-input 
                  v-model="scope.row.length" 
                  placeholder="长度"
                  v-if="scope.row.dataType && ['varchar', 'char'].includes(scope.row.dataType)"
                ></el-input>
                <span v-else>-</span>
              </template>
            </el-table-column>
            
            <el-table-column label="描述" prop="description" min-width="100">
              <template #default="scope">
                <el-input v-model="scope.row.description" placeholder="描述"></el-input>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button
                  type="text"
                  size="small"
                  text-color="#ff4d4f"
                  @click="handleDeleteField(scope.$index)"
                  :disabled="tableForm.fields.length <= 1"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-button 
            type="primary" 
            size="small" 
            @click="handleAddField"
            class="add-field-btn"
          >
            <i class="fas fa-plus"></i> 添加字段
          </el-button>
        </div>

        <!-- 分割线 -->
        <el-divider content-position="left">高级属性</el-divider>

        <!-- 高级属性部分 -->
        <div class="advanced-props">
          <div 
            v-for="(prop, index) in tableForm.props" 
            :key="index" 
            class="property-item"
          >
            <el-input
              v-model="prop.key"
              placeholder="属性名"
              class="property-key"
            ></el-input>
            <el-input
              v-model="prop.value"
              placeholder="属性值"
              class="property-value"
            ></el-input>
            <el-button
              type="text"
              size="small"
              text-color="#ff4d4f"
              @click="handleDeleteProperty(index)"
              :disabled="tableForm.props.length <= 1"
            >
              <i class="fas fa-trash"></i>
            </el-button>
          </div>
          
          <el-button 
            type="primary" 
            size="small" 
            @click="handleAddProperty"
            class="add-property-btn"
          >
            <i class="fas fa-plus"></i> 添加属性
          </el-button>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="showCreateModal = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleConfirm" size="large">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from 'vue';
import { ElMessage } from 'element-plus';

import {
  createCatalogTable,
} from '@/api/data/lake/catalogTable';

const emit = defineEmits(['handleRefreshSidebar']) ; 

const props = defineProps({
  currentCatalogTable: {
    type: Object,
    required: true
  }
});

// 显示新建表对话框
const showCreateModal = ref(false);
const tableFormRef = ref();

// 初始化表信息表单数据
const initTableForm = () => {
  return {
    tableName: '',
    format: '',
    description: '',
    fields: [
      {
        name: '',
        isPrimary: false,
        notNull: false,
        isPartition: false,
        dataType: '',
        length: '',
        description: ''
      }
    ],
    props: [
      {
        key: '',
        value: ''
      }
    ]
  };
};

// 表信息表单数据
const tableForm = reactive(initTableForm());

// 表名验证错误信息
const tableNameError = ref('');

// 监听表名变化，进行实时验证
watch(
  () => tableForm.tableName,
  (val) => {
    if (!val) {
      tableNameError.value = '表名不能为空';
    } else if (val.length > 64) {
      tableNameError.value = '表名长度不能超过64位';
    } else if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(val)) {
      tableNameError.value = '表名必须以字母开头，只能包含字母、数字和下划线';
    } else {
      tableNameError.value = '';
    }
  }
);

// 显示创建模态框
const handleShowCreateModal = () => {

  if(!props.currentCatalogTable.isDirectory){
    ElMessage.error('请选择一个目录');
    return;
  }

  // 重置表单
  resetForm();
  showCreateModal.value = true;
};

// 添加字段
const handleAddField = () => {
  tableForm.fields.push({
    name: '',
    isPrimary: false,
    notNull: false,
    isPartition: false,
    dataType: '',
    length: '',
    description: ''
  });
};

// 删除字段
const handleDeleteField = (index) => {
  if (tableForm.fields.length > 1) {
    tableForm.fields.splice(index, 1);
  }
};

// 添加属性
const handleAddProperty = () => {
  tableForm.props.push({
    key: '',
    value: ''
  });
};

// 删除属性
const handleDeleteProperty = (index) => {
  if (tableForm.props.length > 1) {
    tableForm.props.splice(index, 1);
  }
};

// 确认创建表
const handleConfirm = async () => {
  // 验证表名
  if (tableNameError.value) {
    ElMessage.error('请正确填写表名');
    return;
  }
  
  // 验证至少有一个字段
  if (tableForm.fields.length === 0) {
    ElMessage.error('至少需要一个表字段');
    return;
  }
  
  // 验证字段名
  const hasInvalidField = tableForm.fields.some(field => !field.name);
  if (hasInvalidField) {
    ElMessage.error('请填写所有字段的名称');
    return;
  }
  
  // 验证数据类型
  const hasInvalidType = tableForm.fields.some(field => !field.dataType);
  if (hasInvalidType) {
    ElMessage.error('请为所有字段选择数据类型');
    return;
  }
  
  // 在这里可以获取到完整的表信息
  console.log('表信息:', tableForm);

  tableForm.parentId = props.currentCatalogTable.id;
  tableForm.catalogId = props.currentCatalogTable.catalogId;

  const response = await createCatalogTable(tableForm);
  console.log('response = ' + response);
  
  // 提交成功后关闭对话框
  ElMessage.success('表创建成功');
  emit('handleRefreshSidebar');

  showCreateModal.value = false;

};

// 关闭对话框
const handleClose = () => {
  showCreateModal.value = false;
};

// 重置表单
const resetForm = () => {
  Object.assign(tableForm, initTableForm());
  tableNameError.value = '';
};
</script>

<style lang="scss" scoped>
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

.fields-table-container {
  margin-bottom: 24px;
}

.add-field-btn {
  margin-top: 8px;
}

.advanced-props {
  margin-top: 16px;
}

.property-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  
  .property-key {
    width: 200px;
  }
  
  .property-value {
    flex: 1;
  }
}

.add-property-btn {
  margin-top: 12px;
}

.error-text {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1;
  padding-top: 4px;
}
</style>