<template>
  <div class="app-container">
    <div class="toolbar" style="margin-bottom: 16px;">
      <el-button type="primary" @click="onAdd">新建 Token</el-button>
      <el-button @click="getList">刷新</el-button>
      <el-input
        v-model="query.token"
        placeholder="Token / 备注 / 文件ID"
        style="width: 260px; margin-left: 12px;">
        <template #append>
          <el-button icon="el-icon-search" @click="getList"></el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="list" stripe style="width: 100%" v-loading="loading">
      <!-- 序号 -->
      <el-table-column label="序号" width="80" align="center">
       <template #default="{ $index }">
         {{ ($index + 1) + (pageNum - 1) * pageSize }}
       </template>
     </el-table-column>
      <el-table-column prop="remark" label="说明" min-width="300" />
      <el-table-column prop="token" label="Token" min-width="220" />
      <el-table-column label="目标" min-width="160">
        <template #default="{ row }">
          <span v-if="row.targetType === 0">任意</span>
          <span v-else-if="row.targetType === 1">文件: {{ row.targetId }}</span>
          <span v-else-if="row.targetType === 2">文件夹: {{ row.targetId }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="maxUses" align="center" label="最大次数" width="120" />
      <el-table-column prop="usedCount" align="center" label="已用次数" width="120" />
      <el-table-column prop="expireTime" align="center" label="过期时间" min-width="160">
        <template #default="{ row }">
          <span v-if="row.expireTime">{{ formatDate(row.expireTime) }}</span>
          <span v-else>不过期</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.status === 0" type="success">有效</el-tag>
          <el-tag v-else-if="row.status === 1" type="danger">禁用</el-tag>
          <el-tag v-else type="warning">已用尽/其他</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="addTime" align="center" label="创建时间" min-width="160">
        <template #default="{ row }">
          <span>{{ formatDate(row.addTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="mini" @click="onEdit(row)">编辑</el-button>
          <el-button size="mini" type="primary" @click="onCopy(row.token)">复制</el-button>
          <el-button size="mini" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" style="margin-top: 12px; text-align: right;">
      <el-pagination
        background
        layout="prev, pager, next, sizes, total"
        :page-size="pageSize"
        :current-page="pageNum"
        :page-sizes="[10,20,50,100]"
        :total="total"
        @size-change="onSizeChange"
        @current-change="onPageChange">
      </el-pagination>
    </div>

    <!-- 新建 / 编辑 Dialog -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="Token" prop="token">
          <el-input v-model="form.token" readonly />
          <div style="margin-top:6px;">
            <el-button size="small" type="primary" @click="generateToken">生成 Token</el-button>
            <el-button size="small" @click="onFillRandom">随机填充</el-button>
          </div>
        </el-form-item>

        <el-form-item label="目标类型" prop="targetType">
          <el-select v-model="form.targetType" placeholder="请选择">
            <el-option :label="'任意'" :value="0" />
            <el-option :label="'文件'" :value="1" />
            <el-option :label="'文件夹'" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="目标 ID" prop="targetId">
          <el-input v-model.number="form.targetId" placeholder="当目标类型为文件或文件夹时填写对应 ID" />
        </el-form-item>

        <el-form-item label="最大次数 (0 不限)" prop="maxUses">
          <el-input-number v-model.number="form.maxUses" :min="0" />
        </el-form-item>

        <el-form-item label="过期时间" prop="expireTime">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="选择过期时间 (留空则不过期)"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%;" />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status">
            <el-option :label="'有效'" :value="0" />
            <el-option :label="'禁用'" :value="1" />
            <el-option :label="'已用尽/其他'" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input type="textarea" v-model="form.remark" rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listTokens,
  getToken,
  addToken,
  updateToken,
  deleteToken,
  generateTokenApi
} from '@/api/data/lake/storageToken'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const query = reactive({
  token: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新建 Token')
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  token: '',
  targetType: 0,
  targetId: null,
  maxUses: 0,
  usedCount: 0,
  expireTime: null, // 使用 value-format 字符串，如 "2025-10-07 12:00:00"
  status: 0,
  remark: ''
})

const rules = {
  targetType: [{ required: true, message: '请选择目标类型', trigger: 'change' }]
}

onMounted(() => {
  getList()
})

function formatDate(v) {
  if (!v) return ''
  // 支持字符串、时间戳、Date 对象
  if (typeof v === 'string') return v
  if (typeof v === 'number') {
    const d = new Date(v)
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ` +
           `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`
  }
  if (v instanceof Date) {
    const d = v
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ` +
           `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`
  }
  return String(v)
}

async function getList() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: query.token
    }
    const res = await listTokens(params)
    const data = res.rows
    list.value = data 
    total.value = res.total
  } catch (e) {
    ElMessage.error('获取列表异常')
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  pageNum.value = page
  getList()
}
function onSizeChange(size) {
  pageSize.value = size
  pageNum.value = 1
  getList()
}

function onAdd() {
  dialogTitle.value = '新建 Token'
  isEdit.value = false
  Object.assign(form, {
    id: null,
    token: '',
    targetType: 0,
    targetId: null,
    maxUses: 0,
    usedCount: 0,
    expireTime: null,
    status: 0,
    remark: ''
  })
  dialogVisible.value = true
}

async function onEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑 Token'
  try {
    const res = await getToken(row.id)
    if (res && res.code === 200) {
      const d = res.data || {}
      // expireTime expected as string by date-picker because of value-format
      form.id = d.id ?? null
      form.token = d.token ?? ''
      form.targetType = d.targetType ?? 0
      form.targetId = d.targetId ?? null
      form.maxUses = d.maxUses ?? 0
      form.usedCount = d.usedCount ?? 0
      form.expireTime = d.expireTime ?? null
      form.status = d.status ?? 0
      form.remark = d.remark ?? ''
      dialogVisible.value = true
    } else {
      ElMessage.error(res.msg || '获取数据失败')
    }
  } catch (e) {
    ElMessage.error('获取数据异常')
  }
}

function onCopy(tokenStr) {
  if (!tokenStr) {
    ElMessage.warning('Token 为空')
    return
  }
  navigator.clipboard?.writeText(tokenStr).then(() => {
    ElMessage.success('已复制')
  }).catch(() => {
    // 兼容降级：通过临时 input 复制
    try {
      const input = document.createElement('input')
      input.value = tokenStr
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      ElMessage.success('已复制')
    } catch (err) {
      ElMessage.error('复制失败，请手动复制')
    }
  })
}

function onDelete(row) {
  ElMessageBox.confirm('确认删除该 token 吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        const res = await deleteToken(String(row.id))
        if (res && res.code === 200) {
          ElMessage.success('删除成功')
          getList()
        } else {
          ElMessage.error(res.msg || '删除失败')
        }
      } catch (e) {
        ElMessage.error('删除异常')
      }
    })
    .catch(() => {})
}

async function submitForm() {
  try {
    // Element Plus validate 返回 Promise，校验失败会抛错
    await formRef.value.validate()

    const payload = {
      id: form.id,
      token: form.token,
      targetType: form.targetType,
      targetId: form.targetId,
      maxUses: form.maxUses,
      expireTime: form.expireTime, // 字符串或 null
      status: form.status,
      remark: form.remark
    }
    let res
    if (isEdit.value) {
      res = await updateToken(payload)
    } else {
      res = await addToken(payload)
    }
    if (res && res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      // small delay to allow dialog to close smoothly
      setTimeout(() => getList(), 200)
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (err) {
    // 如果是表单校验失败，Element Plus 会抛出包含 errors 的对象
    if (err && err.errors) {
      ElMessage.warning('请检查表单项')
    } else {
      ElMessage.error('保存异常')
      console.error(err)
    }
  }
}

async function generateToken() {
  try {
    const res = await generateTokenApi()
    if (res && res.code === 200) {
      // 后端返回字符串 token（AjaxResult.data）
      form.token = res.data
      ElMessage.success('生成成功')
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成异常')
  }
}

function onFillRandom() {
  form.token = 't_' + Math.random().toString(36).substring(2, 12)
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 16px;
}
.toolbar { display:flex; align-items:center; }
</style>