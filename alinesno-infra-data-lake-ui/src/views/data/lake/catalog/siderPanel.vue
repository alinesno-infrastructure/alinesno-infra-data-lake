<template>
    <div class="side-type-container">
        <div class="title">行业类型</div>
        <div class="type-list" v-if="sceneList.length > 0">
            <!-- Add All Types option -->
            <div
                :class="{ 'active': activeItem === '' }"
                style="
                    background: rgb(245 247 250);
                    height: 40px;
                    line-height: 25px;
                "
                @mouseover="handleMouseOver('')"
                @mouseout="handleMouseOut('')"
                @click="handleClick('')">
                <i class="fa-solid fa-box"></i> 全部场景类型
            </div> 

            <div
                v-for="item in sceneList"
                :key="item.id"
                :class="{ 'active': activeItem === item.code }"
                @mouseover="handleMouseOver(item.id)"
                @mouseout="handleMouseOut(item.id)"
                @click="handleClick(item.id)">

                <i v-if="item.icon" :class="item.icon"></i>
                <i v-else class="fa-solid fa-box"></i>

                 {{ item.name }} 

                <!--
                <el-check-tag type="info" v-if="item.isMature == '0'" size="small" effect="dark">开发中</el-check-tag>
                --> 
                
            </div>
        </div>
        <div class="empty-list" v-else>
            <el-empty image-size="100" description="当前未配置组织场景类型，未找到场景类型" />
        </div>
    </div>
</template>

<script setup>
import { ref , defineEmits } from 'vue';

import {
    listEnabledDomains
} from '@/api/data/lake/domain'

const emit = defineEmits(['selectType']);

const sceneList = ref([]);

const activeItem = ref(null);
const hoverItem = ref(null);

const handleMouseOver = (id) => {
    hoverItem.value = id;
};

const handleMouseOut = (id) => {
    if (hoverItem.value === id) {
        hoverItem.value = null;
    }
};

const handleClick = (id) => {
    activeItem.value = id;
    console.log('type id = ' + id)
    emit('selectType', id);
};

const handleListEnabledDomains = async () => {
    const res = await listEnabledDomains();
    if (res.code === 200) {
        sceneList.value = res.data;
    }
};

handleListEnabledDomains();

</script>

<style lang="scss" scoped>
.side-type-container {
    padding: 5px;
    width: 100%;
    height: 100%;

    .title {
        font-size: 14px;
        margin-bottom: 10px;
        padding-left: 15px;
        color: #888;
    }

    .type-list {
        font-size: 14px;
        display: flex;
        flex-direction: column;
        gap: 5px;

        div {
            padding: 8px 15px;
            cursor: pointer;
            border-radius: 15px;

            i{
                color: #1d75b0;
                width:22px
            }

            &:hover,
            &.active {
                background-color: var(--el-color-info-light-8);
                color: var(--el-color-info);
            }
        }
    }

    .empty-list {
        display: flex;
        margin-top:30px;
        padding: 20px;
        justify-content: center;
        align-items: center;
    }
}
</style>    