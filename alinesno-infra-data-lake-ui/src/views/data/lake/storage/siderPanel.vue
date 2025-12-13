<template>
    <div class="side-type-container">
        <div class="title">文档类型</div>
        <div class="type-list" v-if="documentTypes.length > 0">
            <!-- 全部类型选项 -->
            <div
                :class="{ 'active': activeItem === '' }"
                @mouseover="handleMouseOver('')"
                @mouseout="handleMouseOut('')"
                @click="handleClick('')">
                <i class="fa-solid fa-box"></i> 全部文档类型
            </div> 

            <div
                v-for="item in documentTypes"
                :key="item.id"
                :class="{ 'active': activeItem === item.code }"
                @mouseover="handleMouseOver(item.id)"
                @mouseout="handleMouseOut(item.id)"
                @click="handleClick(item.code)">
                <i :class="item.icon"></i> {{ item.name }} 
                
                <el-check-tag type="info" v-if="item.isMature === '0'" size="small" effect="dark">开发中</el-check-tag>
            </div>
        </div>
        <div class="empty-list" v-else>
            <el-empty image-size="100" description="当前未配置文档类型，未找到相关类型" />
        </div>
    </div>
</template>

<script setup>
import { ref, defineEmits } from 'vue';

const emit = defineEmits(['selectType']);

// 文档类型列表 - 包含数据库文件、日志文件等类型
const documentTypes = ref([
    {
        "id": 1,
        "name": "Word文档",
        "code": "word",
        "description": "Microsoft Word文档，用于文字处理和文档创作",
        "isMature": "1",
        "icon": "fa-solid fa-file-word"
    },
    {
        "id": 2,
        "name": "Excel表格",
        "code": "excel",
        "description": "Microsoft Excel表格，用于数据处理和分析",
        "isMature": "1",
        "icon": "fa-solid fa-file-excel"
    },
    {
        "id": 3,
        "name": "PPT演示文稿",
        "code": "powerpoint",
        "description": "Microsoft PowerPoint演示文稿，用于制作幻灯片和演示材料",
        "isMature": "1",
        "icon": "fa-solid fa-file-powerpoint"
    },
    {
        "id": 4,
        "name": "PDF文档",
        "code": "pdf",
        "description": "便携式文档格式，用于跨平台文档展示",
        "isMature": "1",
        "icon": "fa-solid fa-file-pdf"
    },
    {
        "id": 5,
        "name": "文本文件",
        "code": "text",
        "description": "纯文本文件，包含未格式化的文本内容",
        "isMature": "1",
        "icon": "fa-solid fa-file-lines"
    },
    {
        "id": 6,
        "name": "图片文件",
        "code": "image",
        "description": "各种格式的图片文件，如JPG、PNG等",
        "isMature": "1",
        "icon": "fa-solid fa-file-image"
    },
    {
        "id": 7,
        "name": "音频文件",
        "code": "audio",
        "description": "各种格式的音频文件，如MP3、WAV等",
        "isMature": "1",
        "icon": "fa-solid fa-file-audio"
    },
    {
        "id": 8,
        "name": "视频文件",
        "code": "video",
        "description": "各种格式的视频文件，如MP4、AVI等",
        "isMature": "1",
        "icon": "fa-solid fa-file-video"
    },
    {
        "id": 9,
        "name": "数据库文件",
        "code": "database",
        "description": "各种数据库格式文件，如MySQL、SQLite、MongoDB等",
        "isMature": "1",
        "icon": "fa-solid fa-database"
    },
    {
        "id": 10,
        "name": "日志文件",
        "code": "log",
        "description": "系统或应用程序生成的日志记录文件",
        "isMature": "1",
        "icon": "fa-solid fa-file"
    },
    {
        "id": 11,
        "name": "压缩文件",
        "code": "archive",
        "description": "各种压缩格式文件，如ZIP、RAR、7Z等",
        "isMature": "1",
        "icon": "fa-solid fa-file-zipper"
    },
    {
        "id": 12,
        "name": "代码文件",
        "code": "code",
        "description": "各种编程语言源代码文件，如Java、Python、JavaScript等",
        "isMature": "1",
        "icon": "fa-solid fa-file-code"
    }
]);

const activeItem = ref(''); // 默认选中全部
const hoverItem = ref(null);

const handleMouseOver = (id) => {
    hoverItem.value = id;
};

const handleMouseOut = (id) => {
    if (hoverItem.value === id) {
        hoverItem.value = null;
    }
};

const handleClick = (code) => {
    activeItem.value = code;
    console.log('选中的文档类型: ' + code)
    emit('selectType', code);
};
</script>

<style lang="scss" scoped>
.side-type-container {
    padding: 5px;
    width: 220px;
    position: absolute;
    height: 100%;
    box-sizing: border-box;
    overflow-y: auto; /* 增加滚动条，避免内容过多时溢出 */

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
            padding: 8px 10px;
            cursor: pointer;
            border-radius: 15px;
            display: flex;
            align-items: center;
            gap: 8px;
            transition: all 0.2s ease;

            i{
                color: #1d75b0;
                width: 20px;
                text-align: center;
            }

            &:hover,
            &.active {
                background-color: var(--el-color-info-light-8);
                color: var(--el-color-info);
            }
        }
    }

    .empty-list {
        margin-top: 30px;
        padding: 20px;
        text-align: center;
    }
}
</style>
    