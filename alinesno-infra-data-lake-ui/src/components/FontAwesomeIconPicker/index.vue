<template>
  <div class="icon-picker">
    <!-- 输入框显示当前选择的图标 -->
    <el-input 
      v-model="displayIcon" 
      placeholder="选择或输入图标类名（如：fa fa-folder）" 
      maxlength="50"
      readonly
      @click="showPicker = true"
    >
      <template #prefix>
        <i :class="selectedIconClass" v-if="selectedIcon"></i>
        <i class="fa fa-question-circle" v-else></i>
      </template>
      <template #suffix>
        <el-button 
          icon="ArrowDown" 
          text 
          size="mini" 
          @click.stop="showPicker = !showPicker"
        ></el-button>
      </template>
    </el-input>
    
    <!-- 图标选择器弹窗 -->
    <teleport to="body">
      <div 
        v-if="showPicker" 
        class="icon-picker-backdrop"
        @click="showPicker = false"
      >
        <div 
          class="icon-picker-popup"
          :style="{ 
            top: `${popupTop}px`, 
            left: `${popupLeft}px`,
            zIndex: 3000 
          }"
          @click.stop
        >
          <div class="icon-picker-content">
            <!-- 搜索框 -->
            <el-input 
              v-model="searchQuery" 
              placeholder="搜索图标..." 
              class="icon-search"
              @input="filterIcons"
              ref="searchInput"
            >
              <template #prefix>
                <i class="fa fa-search"></i>
              </template>
            </el-input>
            
            <!-- 图标分类标签 -->
            <el-tabs 
              v-model="activeCategory" 
              type="card" 
              class="icon-categories"
              @tab-click="handleCategoryChange"
            >
              <el-tab-pane label="全部" name="all"></el-tab-pane>
              <el-tab-pane 
                :label="category.label" 
                :name="category.value" 
                v-for="category in categories" 
                :key="category.value"
              ></el-tab-pane>
            </el-tabs>
            
            <!-- 图标网格 -->
            <div class="icon-grid">
              <div 
                v-for="icon in filteredIcons" 
                :key="icon"
                class="icon-item"
                @click.stop="selectIcon(icon)"
                :class="{ 'icon-item-selected': selectedIcon === icon }"
                :title="`fa ${icon}`"
              >
                <i :class="['fa', icon]"></i>
                <span class="icon-name">fa {{ icon }}</span>
              </div>
              
              <div class="no-results" v-if="filteredIcons.length === 0">
                没有找到匹配的图标
              </div>
            </div>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue';

// 接收父组件传入的图标值（完整类名，如"fa fa-users"）
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
});

// 向父组件 emits 事件（返回完整类名）
const emit = defineEmits(['update:modelValue', 'change']);

// 状态管理
const selectedIcon = ref(''); // 存储图标的后半部分，如"users"
const showPicker = ref(false);
const searchQuery = ref('');
const activeCategory = ref('all');
const popupTop = ref(0);
const popupLeft = ref(0);
const inputRef = ref(null);
const searchInput = ref(null);

// 显示用的完整类名
const selectedIconClass = computed(() => {
  return selectedIcon.value ? ['fa', selectedIcon.value] : [];
});

// 输入框显示的完整类名字符串
const displayIcon = computed({
  get() {
    return selectedIcon.value ? `fa ${selectedIcon.value}` : '';
  },
  set(value) {
    // 处理手动输入的情况
    const parts = value.trim().split(' ').filter(part => part);
    if (parts.length >= 2 && parts[0] === 'fa') {
      selectedIcon.value = parts[1];
    } else if (parts.length === 1 && !parts[0].startsWith('fa-')) {
      selectedIcon.value = parts[0];
    } else {
      selectedIcon.value = '';
    }
  }
});

// 图标分类
const categories = [
  { label: '常用', value: 'common' },
  { label: '文件', value: 'files' },
  { label: '文件夹', value: 'folders' },
  { label: '用户', value: 'users' },
  { label: '系统', value: 'system' },
  { label: '媒体', value: 'media' },
  { label: '其他', value: 'other' }
];

// 图标数据库（存储图标的后半部分）
const iconDatabase = {
  common: [
    'fa-home', 'fa-user', 'fa-cog', 'fa-bell', 'fa-search', 
    'fa-plus', 'fa-minus', 'fa-edit', 'fa-trash', 'fa-save',
    'fa-eye', 'fa-download', 'fa-upload', 'fa-share-alt', 'fa-star'
  ],
  files: [
    'fa-file', 'fa-file-text', 'fa-file-pdf', 'fa-file-word', 
    'fa-file-excel', 'fa-file-powerpoint', 'fa-file-image',
    'fa-file-archive', 'fa-file-code', 'fa-file-video', 'fa-file-audio'
  ],
  folders: [
    'fa-folder', 'fa-folder-open', 'fa-folder-plus', 'fa-folder-minus',
    'fa-folder-check', 'fa-folder-tree'
  ],
  users: [
    'fa-user', 'fa-user-plus', 'fa-user-minus', 'fa-user-circle',
    'fa-users', 'fa-user-friends', 'fa-user-lock', 'fa-user-secret'
  ],
  system: [
    'fa-desktop', 'fa-laptop', 'fa-mobile', 'fa-server', 'fa-database',
    'fa-wifi', 'fa-plug', 'fa-battery-full', 'fa-signal', 'fa-cog'
  ],
  media: [
    'fa-image', 'fa-video', 'fa-music', 'fa-film', 'fa-camera',
    'fa-headphones', 'fa-volume-up', 'fa-volume-off', 'fa-picture-o'
  ],
  other: [
    'fa-tag', 'fa-tags', 'fa-flag', 'fa-map-marker', 'fa-calendar',
    'fa-clock', 'fa-check', 'fa-times', 'fa-info', 'fa-question',
    'fa-exclamation', 'fa-lightbulb', 'fa-fire', 'fa-snowflake',
    'fa-leaf', 'fa-tree', 'fa-heart', 'fa-star'
  ]
};

// 计算所有图标
const allIcons = computed(() => {
  return Object.values(iconDatabase).flat();
});

// 过滤图标
const filteredIcons = computed(() => {
  let result = [];
  
  // 根据分类筛选
  if (activeCategory.value === 'all') {
    result = allIcons.value;
  } else {
    result = iconDatabase[activeCategory.value] || [];
  }
  
  // 根据搜索词筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    // 支持搜索"fa-users"或"users"两种形式
    result = result.filter(icon => 
      icon.toLowerCase().includes(query) || 
      `fa ${icon}`.toLowerCase().includes(query)
    );
  }
  
  return result;
});

// 处理从父组件传入的值
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal) {
      // 从完整类名中提取出图标部分
      const parts = newVal.trim().split(' ').filter(part => part);
      if (parts.length >= 2 && parts[0] === 'fa') {
        selectedIcon.value = parts[1];
      } else if (parts.length === 1) {
        selectedIcon.value = parts[0];
      } else {
        selectedIcon.value = '';
      }
    } else {
      selectedIcon.value = '';
    }
  },
  { immediate: true }
);

// 监听弹窗显示状态变化，计算位置
watch(
  () => showPicker.value,
  (newVal) => {
    if (newVal) {
      calculatePopupPosition();
      // 自动聚焦搜索框
      nextTick(() => {
        if (searchInput.value) {
          searchInput.value.focus();
        }
      });
    } else {
      searchQuery.value = '';
    }
  }
);

// 计算弹窗位置
const calculatePopupPosition = () => {
  nextTick(() => {
    const inputElement = inputRef.value?.$el || document.querySelector('.icon-picker .el-input');
    if (inputElement) {
      const rect = inputElement.getBoundingClientRect();
      // 计算位置，确保弹窗可见
      popupTop.value = rect.bottom + window.scrollY + 5;
      popupLeft.value = rect.left + window.scrollX;
      
      // 检查右侧边界
      const viewportWidth = window.innerWidth;
      if (popupLeft.value + 500 > viewportWidth) {
        popupLeft.value = Math.max(0, viewportWidth - 500);
      }
      
      // 检查底部边界
      const viewportHeight = window.innerHeight;
      if (popupTop.value + 400 > viewportHeight + window.scrollY) {
        popupTop.value = Math.max(0, rect.top + window.scrollY - 400 - 5);
      }
    }
  });
};

// 选择图标 - 发送完整类名给父组件
const selectIcon = (icon) => {
  selectedIcon.value = icon;
  const fullIconClass = `fa ${icon}`; // 组合成完整类名
  emit('update:modelValue', fullIconClass);
  emit('change', fullIconClass);
  showPicker.value = false;
};

// 处理分类变化
const handleCategoryChange = () => {
  const gridElement = document.querySelector('.icon-grid');
  if (gridElement) {
    gridElement.scrollTop = 0;
  }
};

// 过滤图标
const filterIcons = () => {
  const gridElement = document.querySelector('.icon-grid');
  if (gridElement) {
    gridElement.scrollTop = 0;
  }
};

// 窗口大小变化时重新计算位置
const handleResize = () => {
  if (showPicker.value) {
    calculatePopupPosition();
  }
};

// 监听窗口大小变化
onMounted(() => {
  window.addEventListener('resize', handleResize);
  return () => {
    window.removeEventListener('resize', handleResize);
  };
});
</script>

<style lang="scss" scoped>
.icon-picker {
  width: 100%;
  position: relative;
}

// 背景遮罩
.icon-picker-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.1);
  z-index: 2999;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px;
  overflow-y: auto;
}

// 弹窗容器
.icon-picker-popup {
  position: absolute;
  width: 500px;
  background-color: #fff;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
}

.icon-picker-content {
  padding: 10px 0;
}

.icon-search {
  width:80%;
  margin: 0 10px 15px;
}

.icon-categories {
  margin: 0 10px 15px;
  ::v-deep .el-tabs__content {
    display: none;
  }
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 10px;
  padding: 0 10px;
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 10px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 5px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
  user-select: none;
  
  i {
    font-size: 20px;
    margin-bottom: 5px;
    color: #1d75b0;
  }
  
  &:hover {
    background-color: #f5f7fa;
    transform: translateY(-2px);
  }
}

.icon-item-selected {
  background-color: #e6f7ff !important;
  border: 1px solid #91d5ff;
  
  i {
    color: #1890ff;
  }
}

.icon-name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  color: #606266;
}

.no-results {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 14px;
}
</style>
