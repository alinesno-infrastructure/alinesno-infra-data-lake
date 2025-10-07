<template>
  <div class="app-container">
    <!-- 主要内容区域 -->
    <el-container>
      <el-main class="main-content">
        <!-- 页面标题和操作按钮 -->
        <div class="page-header">
          <div class="page-title">
            <h2>业务域分类</h2>
            <p>管理数据湖中的业务域分类，包括添加、编辑和删除操作</p>
          </div>
          <div class="page-actions">
            <el-button @click="handleAdd" type="primary" icon="Plus">
              添加分类
            </el-button>
          </div>
        </div>
        
        <!-- 搜索和筛选区域 -->
        <div class="filter-card">
          <div class="filter-bar">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-input 
                  v-model="queryParams.searchKey" 
                  placeholder="搜索分类名称..." 
                  prefix-icon="Search"
                  clearable
                  @keyup.enter="handleSearch"
                ></el-input>
              </el-col>
              <el-col :span="4">
                <el-button 
                  @click="handleSearch" 
                  type="primary" 
                  icon="Search"
                >
                  搜索
                </el-button>
                <el-button 
                  @click="resetSearch" 
                  icon="RefreshRight"
                  plain
                  style="margin-left: 8px"
                >
                  重置
                </el-button>
              </el-col>
            </el-row>
          </div>
        </div>
        
        <!-- 分类列表 -->
          <el-table 
            v-loading="loading"
            :data="domainList" 
            row-key="id"
          >
            <el-table-column type="selection" width="55"></el-table-column>
            <el-table-column type="index" label="序号" width="55"></el-table-column>
            
            <el-table-column 
              prop="name" 
              label="分类名称" 
              min-width="150"
            >
              <template #default="scope">
                <div style="display: flex; align-items: center;">
                  <i v-if="scope.row.icon" :class="scope.row.icon" style="margin-right: 8px;"></i>
                  <i v-else class="fa fa-folder" style="margin-right: 8px;"></i>
                  <span>{{ scope.row.name }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column 
              prop="sortOrder" 
              label="排序" 
              width="80"
              align="center"
            ></el-table-column>
            
            <el-table-column 
              prop="description" 
              label="描述" 
              min-width="200"
              show-overflow-tooltip
            ></el-table-column>
            
            <el-table-column 
              label="状态" 
              width="100"
              align="center"
            >
              <template #default="scope">
                <el-switch
                  v-model="scope.row.hasStatus"
                  :active-value="0"
                  :inactive-value="1"
                  @change="handleStatusChange(scope.row)"
                ></el-switch>
              </template>
            </el-table-column>
            
            <el-table-column 
              prop="addTime" 
              label="创建时间" 
              width="160"
            ></el-table-column>
            
            <el-table-column 
              label="操作" 
              width="180"
              align="center"
              fixed="right"
            >
              <template #default="scope">
                <el-button 
                  @click="handleView(scope.row)" 
                  text bg
                  icon="View"
                  type="primary"
                  plain
                ></el-button>
                <el-button 
                  @click="handleEdit(scope.row)" 
                  text bg
                  icon="Edit"
                  type="warning"
                  plain
                ></el-button>
                <el-button 
                  @click="handleDelete(scope.row)" 
                  text bg
                  icon="Delete"
                  type="danger"
                  plain
                ></el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="queryParams.pageNum"
              v-model:page-size="queryParams.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            ></el-pagination>
          </div>
      </el-main>
    </el-container>
    
    <!-- 添加/编辑分类模态框 -->
    <el-dialog 
      v-model="open" 
      :title="title"
      :width="500"
      append-to-body
      @close="cancel"
    >
      <el-form 
        ref="domainFormRef" 
        :model="form" 
        :rules="rules"
        label-width="80px"
      >
        <el-form-item 
          label="分类图标" 
          prop="icon"
        >
          <!-- <el-input v-model="form.icon" placeholder="请输入分类图标" /> -->
          <FontAwesomeIconPicker 
                  v-model="form.icon" 
                  placeholder="选择一个图标"
                  />
        </el-form-item>

        <el-form-item 
          label="分类名称" 
          prop="name"
        >
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        
        <el-form-item 
          label="排序" 
          prop="sortOrder"
        >
          <el-input-number 
            v-model="form.sortOrder" 
            :min="1"
            controls-position="right"
          ></el-input-number>
        </el-form-item>
        
        <el-form-item 
          label="状态" 
          prop="status"
        >
          <!-- <el-radio-group v-model="form.status">
            <el-radio label="0"></el-radio>
            <el-radio label="1"></el-radio>
          </el-radio-group> -->
          <el-radio-group v-model="form.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item 
          label="分类描述" 
          prop="description"
        >
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入内容"
          ></el-input>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 查看分类详情模态框 -->
    <el-dialog 
      v-model="openView" 
      title="分类详情"
      :width="500"
      append-to-body
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="分类名称">{{ form.name }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ form.sortOrder }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="form.status === 0 ? 'success' : 'danger'">
            {{ form.status === 0 ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ form.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ form.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="分类描述">{{ form.description || '无描述' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { Folder, View, Edit, Delete, Search, RefreshRight, Plus } from '@element-plus/icons-vue'
import FontAwesomeIconPicker from '@/components/FontAwesomeIconPicker/index.vue';

import { 
  listDomains, 
  addDomain, 
  updateDomain, 
  delDomain, 
  getDomain,
  checkNameExist
} from '@/api/data/lake/domain'

// 获取当前实例
const { proxy } = getCurrentInstance()

const domainList = ref([])
const loading = ref(true)
const total = ref(0)
const title = ref('')
const open = ref(false)
const openView = ref(false)
const domainFormRef = ref(null)

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  searchKey: null 
})

const form = reactive({
  id: undefined,
  name: '',
  sortOrder: 1,
  status: 0,
  description: ''
})

const rules = reactive({
  name: [
    { required: true, message: '分类名称不能为空', trigger: 'blur' },
    { min: 1, max: 100, message: '分类名称长度必须介于 1 和 100 之间', trigger: 'blur' },
    { validator: checkNameUnique, trigger: 'blur' }
  ],
  sortOrder: [
    { required: true, message: '排序不能为空', trigger: 'blur' }
  ]
})

// 名称唯一性验证
function checkNameUnique(rule, value, callback) {
  if (!value) {
    callback()
    return
  }
  
  const id = form.id || null
  checkNameExist(value, id).then(response => {
    if (!response.data) {
      callback(new Error('分类名称已存在'))
    } else {
      callback()
    }
  }).catch(() => {
    callback()
  })
}

/** 搜索按钮操作 */
function handleSearch() {
  queryParams.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetSearch() {
  queryParams.searchKey = ''
  queryParams.pageNum = 1
  getList()
}

/** 分页大小改变 */
function handleSizeChange(val) {
  queryParams.pageSize = val
  getList()
}

/** 当前页码改变 */
function handleCurrentChange(val) {
  queryParams.pageNum = val
  getList()
}

/** 获取分类列表 */
function getList() {
  loading.value = true
  listDomains(queryParams).then(response => {
    console.log('获取列表成功:', response.code)
    if (response.code === 200) {
      domainList.value = response.rows || []
      total.value = response.total
    } else {
      ElMessage.error(response.msg || '获取列表失败')
    }
    loading.value = false
  }).catch(error => {
    console.error('获取列表失败:', error)
    ElMessage.error('获取列表失败')
    loading.value = false
  })
}

/** 状态修改 */
function handleStatusChange(row) {
  let text = row.status === 0 ? '启用' : '停用'
  ElMessageBox.confirm(`确认要"${text}" "${row.name}" 分类吗?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return updateDomain(row)
  }).then(response => {
    if (response.code === 200) {
      ElMessage.success(`${text}成功`)
      getList()
    } else {
      ElMessage.error(response.msg || `${text}失败`)
      // 恢复原来的状态
      row.status = row.status === 0 ? 1 : 0
    }
  }).catch(() => {
    // 取消操作，恢复原来的状态
    row.status = row.status === 0 ? 1 : 0
  })
}

/** 查看按钮操作 */
function handleView(row) {
  resetForm()
  const id = row.id
  getDomain(id).then(response => {
    if (response.code === 200) {
      Object.assign(form, response.data)
      openView.value = true
      title.value = '分类详情'
    } else {
      ElMessage.error(response.msg || '获取详情失败')
    }
  }).catch(error => {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  })
}

/** 新增按钮操作 */
function handleAdd() {
  resetForm()
  open.value = true
  title.value = '添加分类'
}

/** 编辑按钮操作 */
function handleEdit(row) {
  resetForm()
  const id = row.id
  getDomain(id).then(response => {
    if (response.code === 200) {
      Object.assign(form, response.data)
      open.value = true
      title.value = '修改分类'
    } else {
      ElMessage.error(response.msg || '获取详情失败')
    }
  }).catch(error => {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const id = row.id
  ElMessageBox.confirm(`是否确认删除分类名称为"${row.name}"的数据项?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delDomain(id)
  }).then(response => {
    if (response.code === 200) {
      ElMessage.success('删除成功')
      getList()
    } else {
      ElMessage.error(response.msg || '删除失败')
    }
  }).catch(() => {})
}

/** 提交表单 */
function submitForm() {
  domainFormRef.value.validate(valid => {
    if (valid) {
      if (form.id) {
        // 编辑
        updateDomain(form).then(response => {
          if (response.code === 200) {
            ElMessage.success('修改成功')
            open.value = false
            getList()
          } else {
            ElMessage.error(response.msg || '修改失败')
          }
        }).catch(error => {
          console.error('修改失败:', error)
          ElMessage.error('修改失败')
        })
      } else {
        // 新增
        addDomain(form).then(response => {
          if (response.code === 200) {
            ElMessage.success('新增成功')
            open.value = false
            getList()
          } else {
            ElMessage.error(response.msg || '新增失败')
          }
        }).catch(error => {
          console.error('新增失败:', error)
          ElMessage.error('新增失败')
        })
      }
    }
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  resetForm()
}

/** 重置表单 */
function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    sortOrder: 1,
    status: 0,
    description: ''
  })
  if (domainFormRef.value) {
    domainFormRef.value.resetFields()
  }
}

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.app-container {
  min-height: calc(100vh - 50px);
  padding: 20px;
}

.main-content {
  padding: 0 !important;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  .page-title {
    h2 {
      font-size: 20px;
      font-weight: 600;
      margin: 0 0 8px 0;
    }
    
    p {
      color: #86909C;
      margin: 0;
      font-size: 14px;
    }
  }
}

.filter-card {
  width: calc(100% - 20px);
  margin-bottom: 20px;
}

.filter-bar {
  padding: 16px 0;
}

.category-table-card {
  .el-table {
    margin-bottom: 16px;
  }
}

.pagination-container {
  text-align: right;
  padding: 10px 0;
}

// 响应式调整
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    
    .page-actions {
      margin-top: 16px;
    }
  }
  
  .filter-bar .el-col {
    margin-bottom: 10px;
  }
}
</style>