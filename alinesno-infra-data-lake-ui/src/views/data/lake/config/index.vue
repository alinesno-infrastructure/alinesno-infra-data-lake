<template>
  <div class="app-container">
    <div class="label-title">
      <div class="tip">数据湖平台配置</div>
      <div class="sub-tip">Iceberg 元数据管理（MinIO / Spark 在后台固定）</div>
    </div>

    <div class="form-container">
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="160px"
        size="large"
        v-loading="loading"
        class="demo-form"
      >
        <!-- 展示后台固定的 MinIO / Spark 信息 -->
        <div class="sub-section-title">系统信息（后台固定）</div>
        <el-form-item label="MinIO Endpoint">
          <el-input v-model="sysInfo.minioEndpoint" readonly></el-input>
        </el-form-item>
        <el-form-item label="MinIO Bucket">
          <el-input v-model="sysInfo.minioBucket" readonly></el-input>
        </el-form-item>
        <el-form-item label="Spark Master">
          <el-input v-model="sysInfo.sparkMaster" readonly></el-input>
        </el-form-item>
        <el-form-item label="服务状态">
          <el-tag :type="sysInfo.up ? 'success' : 'danger'">{{ sysInfo.up ? '正常' : '异常' }}</el-tag>
          <span style="margin-left:12px;color:#666;">（来自后台：若需要修改请在运维端调整）</span>
        </el-form-item>

        <!-- 可配置项：Iceberg / Catalog / Metastore -->
        <div class="sub-section-title">元数据配置</div>

        <el-form-item label="Catalog 类型" prop="catalogType">
          <el-select v-model="form.catalogType" placeholder="选择 Catalog 类型">
            <el-option label="Iceberg (HadoopCatalog)" value="hadoop"></el-option>
            <el-option label="Iceberg (HiveCatalog)" value="hive"></el-option>
            <el-option label="自定义" value="custom"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="默认数据库" prop="defaultDatabase">
          <el-input v-model="form.defaultDatabase" placeholder="例如：default_db"></el-input>
        </el-form-item>

        <el-form-item label="Warehouse 路径" prop="warehousePath">
          <el-input v-model="form.warehousePath" placeholder="例如：s3a://bucket/warehouse/"></el-input>
        </el-form-item>

        <el-form-item label="Metastore 类型" prop="metastoreType">
          <el-select v-model="form.metastoreType" placeholder="选择 Metastore">
            <el-option label="无 (仅使用 Catalog)" value="none"></el-option>
            <el-option label="Hive Metastore (Thrift)" value="hive"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.metastoreType === 'hive'" label="Thrift URI" prop="metastoreUri">
          <el-input v-model="form.metastoreUri" placeholder="thrift://hive-metastore:9083"></el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="testMetastore" v-if="form.metastoreType === 'hive'">测试 Metastore</el-button>
          <el-button type="info" @click="testCatalog">测试 Catalog</el-button>
        </el-form-item>

        <div class="sub-section-title">运行时配置</div>

        <el-form-item label="Catalog 刷新间隔(秒)" prop="catalogRefreshInterval">
          <el-input-number v-model="form.catalogRefreshInterval" :min="10" :max="86400"></el-input-number>
        </el-form-item>

        <el-form-item label="开启访问鉴权">
          <el-switch v-model="form.enableAuth" :active-value="1" :inactive-value="0"></el-switch>
        </el-form-item>

        <el-form-item label="备注">
          <el-input type="textarea" v-model="form.note" rows="3" placeholder="备注或说明"></el-input>
        </el-form-item>

        <br/>

        <el-form-item>
          <el-button type="primary" @click="submitForm">保存</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, getCurrentInstance } from 'vue';

const { proxy } = getCurrentInstance();
const formRef = ref(null);
const loading = ref(false);

// 系统信息（MinIO / Spark 后台固定，前端只展示）
const sysInfo = reactive({
  minioEndpoint: '',
  minioBucket: '',
  sparkMaster: '',
  up: true,
});

// 可配置表单
const form = reactive({
  catalogType: 'hadoop',
  defaultDatabase: 'default',
  warehousePath: 's3a://bucket/warehouse/',
  metastoreType: 'none',
  metastoreUri: '',
  catalogRefreshInterval: 60,
  enableAuth: 0,
  note: '',
});

const rules = {
  catalogType: [{ required: true, message: '请选择 Catalog 类型', trigger: 'change' }],
  warehousePath: [
    { required: true, message: '请输入 Warehouse 路径', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value) return callback(new Error('请输入 Warehouse 路径'));
        callback();
      },
      trigger: 'blur',
    },
  ],
  metastoreUri: [
    {
      validator: (rule, value, callback) => {
        if (form.metastoreType === 'hive' && !value) {
          return callback(new Error('请输入 Metastore Thrift URI'));
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
  sparkMaster: [], // Spark 在后台固定，前端不编辑
};

// 消息
function msgSuccess(text) { proxy?.$message?.({ message: text, type: 'success' }); }
function msgError(text) { proxy?.$message?.({ message: text, type: 'error' }); }

// 加载当前配置与后台固定信息
async function loadConfig() {
  loading.value = true;
  try {
    // TODO: 替换为真实接口
    // const resp = await axios.get('/api/datalake/config');
    // const sys = await axios.get('/api/datalake/system-info');
    // 示意数据：
    await new Promise(r => setTimeout(r, 300));
    // 从后端读取的示例（请替换）
    const respData = {
      catalogType: 'hadoop',
      defaultDatabase: 'default',
      warehousePath: 's3a://iceberg-bucket/warehouse/',
      metastoreType: 'none',
      catalogRefreshInterval: 60,
      enableAuth: 0,
      note: '示例配置',
    };
    const sys = {
      minioEndpoint: 'http://minio.prod.company.com',
      minioBucket: 'iceberg-prod',
      sparkMaster: 'spark://spark-master:7077',
      up: true,
    };

    // 赋值到表单与系统信息
    Object.assign(form, respData);
    Object.assign(sysInfo, sys);
  } catch (err) {
    msgError('加载配置失败：' + (err.message || err));
  } finally {
    loading.value = false;
  }
}

// 测试 Metastore（如果启用）
async function testMetastore() {
  if (form.metastoreType !== 'hive') {
    msgSuccess('未配置 Hive Metastore，无需测试');
    return;
  }
  loading.value = true;
  try {
    // TODO: 调用后端测试接口：POST /api/datalake/test-metastore { uri }
    await new Promise(r => setTimeout(r, 500));
    msgSuccess('Metastore 连接成功');
  } catch (err) {
    msgError('Metastore 测试失败：' + (err.message || err));
  } finally {
    loading.value = false;
  }
}

// 测试 Catalog（校验 warehouse 等）
async function testCatalog() {
  loading.value = true;
  try {
    // TODO: 调用后端测试接口：POST /api/datalake/test-catalog { catalogType, warehousePath }
    await new Promise(r => setTimeout(r, 500));
    msgSuccess('Catalog 测试通过');
  } catch (err) {
    msgError('Catalog 测试失败：' + (err.message || err));
  } finally {
    loading.value = false;
  }
}

// 保存配置（只保存元数据层配置）
function submitForm() {
  formRef.value.validate(async (valid) => {
    if (!valid) {
      msgError('请先修正表单错误');
      return;
    }
    loading.value = true;
    try {
      // TODO: 替换为后端保存接口：POST /api/datalake/config
      // await axios.post('/api/datalake/config', form);
      await new Promise(r => setTimeout(r, 700));
      msgSuccess('配置已保存');
      // 可再次加载以保证与后台同步
      await loadConfig();
    } catch (err) {
      msgError('保存失败：' + (err.message || err));
    } finally {
      loading.value = false;
    }
  });
}

function resetForm() {
  formRef.value.resetFields();
  loadConfig();
}

onMounted(() => {
  loadConfig();
});
</script>

<style scoped lang="scss">
.form-container {
  max-width: 980px;
  margin: 20px auto;
}
.label-title { text-align: center; margin: 10px auto 20px; }
.tip { font-size: 26px; font-weight: bold; padding-bottom: 10px; }
.sub-tip { color: #666; font-size: 13px; padding: 8px; }
.sub-section-title { margin: 18px 0 6px; font-weight: 600; color: #333; }
.demo-form { background: #fff; padding: 16px; border-radius: 6px; }
</style>