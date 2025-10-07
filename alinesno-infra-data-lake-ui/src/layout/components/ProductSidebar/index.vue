<template>
  <div class="siderbar">
    <el-menu :default-active="activeMenu" class="el-menu-vertical" :collapse="isCollapse" @open="handleOpen" @close="handleClose">

        <el-menu-item :index="item.id" @click="openServiceList(item.link)" v-for="item in menuItems" :key="item.id"  class="aip-menu-item">
          <i :class="item.icon"></i>
          <span>
            {{ item.desc }}
          </span>
        </el-menu-item>
    </el-menu>

    <el-menu style="" class="el-menu-vertical acp-suggest" :collapse="isCollapse" @open="handleOpen" @close="handleClose">
        <el-menu-item :index="item.id" @click="openServiceList(item.link)" class="aip-menu-item" v-for="item in footerMenuItems" :key="item.id">
          <i :class="item.icon"></i>
          <span>
            {{ item.desc }}
          </span>
      </el-menu-item>
    </el-menu>

  </div>
</template>

<script setup>

const createChildComp = ref(null);
const createScreenComp = ref(null);

const dialogVisible = ref(false)

const router = useRouter();
const route = useRoute();

// 菜单列表
const menuItems = ref([
  { id: '1', icon: 'fa-solid fa-house-user', link: '/index', desc: '仪盘表' }, 
  { id: '3', icon: 'fa-solid fa-feather', link: '/lake/data/lake/catalog/index', desc: '业务域' },
  { id: '4', icon: 'fa-solid fa-database', link: '/lake/data/lake/type/index', desc: '域分类 ' },
  { id: '5', icon: 'fa-solid fa-hard-drive', link: '/lake/data/lake/storage/index', desc: '存储' },
  { id: '12', icon: 'fa-solid fa-computer', link: '/lake/data/lake/analyse/index', desc: '监控' },
]);

// Footer菜单列表
const footerMenuItems = ref([
  { id: '16', icon: 'fa-solid fa-shield-halved', link: '/config/data/lake/token/index', desc: '请求Token' },
  { id: '13', icon: 'fa-solid fa-history ', link: '/config/data/lake/apiRecord/index', desc: '操作记录' },
  { id: '15', icon: 'fa-solid fa-cog', link: '/config/data/lake/config/index', desc: ' 配置 ' },
]);

// 打开服务市场
function openServiceList(_path) {
  router.push({ path: _path });
}

// 打开客户配置
function jumpToConstomTheme() {
  router.push({ path: "/dashboard/dashboardTheme" });
}

// 添加频道
function addChannel(){
  createChildComp.value.handleOpenChannel(true);
}

// 添加场景
function addScreen(){
  createScreenComp.value.handleOpenChannel(true);
}

// 打开首页
function jumpTo() {
  router.push({ path: "/index" });
}

// 打开智能客服
function openSmartService() {
  router.push({ path: "/dashboard/smartService" });
}

const activeMenu = computed(() => {
  const currentPath = route.path
  const item = menuItems.value.find(item => {
    return currentPath.startsWith(item.link) // 匹配路由前缀
  })
  return item ? item.id : '1'
})

</script>

<style lang="scss" scoped>
.el-menu-vertical:not(.el-menu--collapse) {
  width: 70px;
}

.acp-suggest {
  bottom: 20px;
  position: absolute;
}

.siderbar {
  float: left;
  height: 100%;
  width: 70px;
  border-right: 1px solid #e6e6e6;
  padding-top: 40px;
  overflow: hidden;
  background-color: #fff;
  position: fixed;
  margin-top: 10px;
  margin-bottom: 20px;
  z-index: 10;
}

.aip-menu-item {

  display: flex;
  flex-direction: column;
  gap: 5px;
  line-height: 1.2rem;
  padding-top: 0px;
  margin: 5px;
  border-radius: 10px;
  justify-content: center;

  span{
    font-size:12px;
    color: #888;
  }
}
</style>
