<template>
    <div>
        <el-button type="primary" size="large"  @click="addCatalog">
            添加目录
        </el-button>
        
        <!-- 目录添加/编辑弹窗 -->
        <el-dialog
            :title="formData.id ? '编辑目录' : '添加目录'"
            v-model="dialogVisible"
            width="600px"
            :before-close="handleClose"
        >
            <el-form ref="catalogForm" :model="formData" :rules="rules" labelPosition="top" label-width="100px"
            size="large"
            >

                <!-- 所属业务域 -->
                <el-form-item label="业务域" prop="domainId">
                  <el-select
                    v-model="formData.domainId"
                    placeholder="请选择所属业务域"
                    size="large"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="option in domainOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    ></el-option>
                  </el-select>
                </el-form-item>

                <!-- 目录名称 -->
                <el-form-item label="目录名称" prop="catalogName">
                    <el-input 
                        v-model="formData.catalogName" 
                        placeholder="请输入目录名称" 
                        maxlength="100"
                    ></el-input>
                </el-form-item>
                
                <!-- 描述 -->
                <el-form-item label="描述" prop="description">
                    <el-input 
                        v-model="formData.description" 
                        placeholder="请输入描述" 
                        type="textarea" 
                        rows="3"
                        resize="none"
                        maxlength="500"
                    ></el-input>
                </el-form-item>
                
                <!-- 图标 -->
                <el-form-item label="图标" prop="icon">
                    <FontAwesomeIconPicker 
                            v-model="formData.icon" 
                            placeholder="选择一个图标"
                        />
                </el-form-item>
            </el-form>
            
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitForm" :loading="loading">
                    {{ loading ? '提交中...' : '确定' }}
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, defineExpose } from 'vue';
import { ElMessage } from 'element-plus';

import FontAwesomeIconPicker from '@/components/FontAwesomeIconPicker/index.vue';

import {
    listEnabledDomains
} from '@/api/data/lake/domain'

// 导入创建和更新接口
import { 
    createCatalog,
    updateCatalog
} from '@/api/data/lake/catalog';

const emit = defineEmits(['addSuccess', 'updateSuccess'])

// 弹窗显示状态
const dialogVisible = ref(false);
const loading = ref(false);

// 表单数据
const formData = reactive({
    id: null,
    catalogName: '',  // 目录名称
    description: '',  // 描述
    icon: ''          // 图标
});

// 业务域下拉选项
const domainOptions = ref([]);

// 表单验证规则
const rules = reactive({
    catalogName: [
        { required: true, message: '请输入目录名称', trigger: 'blur' },
        { max: 100, message: '目录名称不能超过100个字符', trigger: 'blur' }
    ],
    description: [
        { max: 500, message: '描述不能超过500个字符', trigger: 'blur' }
    ],
    icon: [
        { max: 50, message: '图标标识不能超过50个字符', trigger: 'blur' }
    ],
    domainId: [
        { required: true, message: '请选择业务域', trigger: 'change' }
      ]
});

// 表单引用
const catalogForm = ref(null);

// 打开添加目录弹窗
const addCatalog = () => {
    // 重置表单
    if (catalogForm.value) {
        catalogForm.value.resetFields();
    }
    formData.id = null;
    formData.catalogName = '';
    formData.description = '';
    formData.icon = '';
    
    // 显示弹窗
    dialogVisible.value = true;
};

// 打开编辑目录弹窗
const editCatalog = (catalogData) => {
    // 重置表单
    if (catalogForm.value) {
        catalogForm.value.resetFields();
    }
    
    // 填充表单数据
    formData.id = catalogData.id;
    formData.catalogName = catalogData.catalogName;
    formData.description = catalogData.description || '';
    formData.icon = catalogData.icon || '';
    
    // 显示弹窗
    dialogVisible.value = true;
};

// 关闭弹窗回调
const handleClose = () => {
    dialogVisible.value = false;
};

// 提交表单
const submitForm = async () => {
    try {
        const valid = await catalogForm.value.validate();
        if (!valid) {
            return;
        }
        
        loading.value = true;
        
        let response;
        // 根据是否有id判断是新增还是编辑
        if (formData.id) {
            // 调用更新目录接口
            response = await updateCatalog({
                id: formData.id,
                catalogName: formData.catalogName,
                description: formData.description,
                icon: formData.icon
            });
        } else {
            // 调用创建目录接口
            response = await createCatalog({
                catalogName: formData.catalogName,
                description: formData.description,
                icon: formData.icon
            });
        }
        
        if (response.code === 200) {
            ElMessage.success(formData.id ? '目录更新成功' : '目录创建成功');
            dialogVisible.value = false;
            
            // 触发对应的事件，通知父组件刷新数据
            if (formData.id) {
                emit('updateSuccess');
            } else {
                emit('addSuccess');
            }
        } else {
            ElMessage.error(response.msg || (formData.id ? '更新目录失败' : '创建目录失败'));
        }
    } catch (error) {
        console.error(formData.id ? '更新目录失败:' : '创建目录失败:', error);
        ElMessage.error(error.response?.data?.msg || error.message || (formData.id ? '更新目录失败' : '创建目录失败'));
    } finally {
        loading.value = false;
    }
};

// 初始化时加载业务域数据（与添加/编辑弹窗逻辑兼容）
const loadDomainOptions = async () => {
  try {
    const res = await listEnabledDomains();
    if (res.code === 200) {
      // 格式化选项为 el-select 所需格式（label 显示名称，value 绑定 domainId）
      domainOptions.value = res.data.map(item => ({
        label: item.name,
        value: item.id
      }));
    }
  } catch (error) {
    console.error('加载业务域失败:', error);
    ElMessage.error('加载业务域选项失败，请刷新重试');
  }
};

nextTick(() => {
  loadDomainOptions();
});

// 暴露方法给父组件调用
defineExpose({
    addCatalog,
    editCatalog
});

</script>

<style lang="scss" scoped>
.view-btn {
    background: none;
    border: none;
    color: #6b7280;
    cursor: pointer;
    transition: all 0.2s ease;
}

.view-btn.active {
    color: #1d75b0;
    background-color: rgba(22, 93, 255, 0.1);
}

.view-btn:hover:not(.active) {
    color: #374151;
}

/* 弹窗内表单样式补充 */
::v-deep .el-form-item {
    margin-bottom: 15px;
}

::v-deep .el-textarea__inner {
    resize: vertical;
}
</style>
