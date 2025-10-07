<template>
  <el-drawer 
    :model-value="showUploadProgress" 
    direction="rtl" 
    :with-header="false"
    :width="320"
    class="upload-progress"
  >
    <div class="progress-header">
      <h4>正在上传</h4>
      <el-button 
        icon="Close" 
        size="small" 
        circle 
        class="progress-close"
        @click="cancelUpload"
      ></el-button>
    </div>
    <p class="progress-filename">{{ uploadFileName }}</p>
    <el-progress 
      :percentage="uploadProgress" 
      stroke-width="6"
      class="progress-bar"
    ></el-progress>
    <div class="progress-info">
      <span class="progress-percent">{{ uploadProgress }}%</span>
      <span class="progress-speed">{{ uploadSpeed }}</span>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits } from 'vue'

const props = defineProps({
  showUploadProgress: {
    type: Boolean,
    default: false
  },
  uploadFileName: {
    type: String,
    default: ''
  },
  uploadProgress: {
    type: Number,
    default: 0
  },
  uploadSpeed: {
    type: String,
    default: '0 KB/s'
  }
})

const emit = defineEmits(['update:showUploadProgress', 'cancel'])

// 取消上传
const cancelUpload = () => {
  emit('update:showUploadProgress', false)
  emit('cancel')
}
</script>

<style lang="scss" scoped>
.upload-progress {
  padding: 20px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h4 {
      font-size: 16px;
      color: #1d2129;
      margin: 0;
    }

    .progress-close {
      color: #86909c;

      &:hover {
        color: #f53f3f;
        background-color: rgba(245, 63, 63, 0.1);
      }
    }
  }

  .progress-filename {
    font-size: 14px;
    color: #1d2129;
    margin: 0 0 16px 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .progress-bar {
    margin-bottom: 12px;
  }

  .progress-info {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #86909c;
  }
}
</style>