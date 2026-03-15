<template>
  <div class="project-container">
    <!-- Header Section -->
    <div class="header-section">
      <div class="title-area">
        <h1>文档管理</h1>
        <p>管理并组织您的项目文件</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" class="new-project-btn">
          <el-icon class="mr-2"><Plus /></el-icon> 新建项目
          <el-icon class="ml-2 border-l pl-2 border-white/30"><ArrowDown /></el-icon>
        </el-button>
        <div class="top-icons">
          <el-icon class="icon-btn"><Moon /></el-icon>
          <el-icon class="icon-btn"><Bell /></el-icon>
          <el-avatar :size="32" src="https://i.pravatar.cc/100?u=admin" />
        </div>
      </div>
    </div>

    <!-- Main Content Card -->
    <div class="content-card glass-card">
      <!-- Search and Filter Bar -->
      <div class="toolbar">
        <div class="search-box">
          <el-input
              v-model="searchQuery"
              placeholder="搜索文档、标签或文件夹..."
              class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #suffix>
              <span class="command-hint">⌘ F</span>
            </template>
          </el-input>
        </div>
        <div class="filter-actions">
          <el-select v-model="sortBy" placeholder="排序：日期" class="filter-select">
            <el-option label="排序：日期" value="date" />
            <el-option label="排序：名称" value="name" />
            <el-option label="排序：大小" value="size" />
          </el-select>
          <el-select v-model="groupBy" placeholder="分组：无" class="filter-select">
            <el-option label="分组：无" value="none" />
            <el-option label="分组：类型" value="type" />
            <el-option label="分组：作者" value="author" />
          </el-select>
          <div class="view-toggle">
            <el-button-group>
              <el-button :class="{ active: viewType === 'grid' }" @click="viewType = 'grid'">
                <el-icon><Grid /></el-icon>
              </el-button>
              <el-button :class="{ active: viewType === 'list' }" @click="viewType = 'list'">
                <el-icon><Expand /></el-icon>
              </el-button>
            </el-button-group>
          </div>
          <el-button type="primary" class="upload-btn">
            <el-icon class="mr-2"><Upload /></el-icon> 上传
          </el-button>
          <el-button class="new-folder-btn">
            <el-icon class="mr-2"><FolderAdd /></el-icon> 新建文件夹
          </el-button>
        </div>
      </div>

      <!-- Document Count -->
      <div class="count-info">
        正在显示 {{ documents.length }} 个文档中的 {{ filteredDocuments.length }} 个
      </div>

      <!-- Documents Grid -->
      <div v-if="viewType === 'grid'" class="documents-grid">
        <div v-for="doc in filteredDocuments" :key="doc.id" class="doc-card">
          <div class="doc-icon-wrapper" :class="doc.type">
            <el-icon v-if="doc.type === 'txt'"><Document /></el-icon>
            <el-icon v-else-if="doc.type === 'docx'"><EditPen /></el-icon>
            <el-icon v-else-if="doc.type === 'pdf'"><Files /></el-icon>
            <el-icon v-else-if="doc.type === 'xlsx'"><DataAnalysis /></el-icon>
            <el-icon v-else-if="doc.type === 'figma'"><Picture /></el-icon>
            <el-icon v-else-if="doc.type === 'png'"><PictureFilled /></el-icon>
            <el-icon v-else-if="doc.type === 'mp4'"><VideoCamera /></el-icon>
            <el-icon v-else-if="doc.type === 'zip'"><Box /></el-icon>
            <el-icon v-else><Document /></el-icon>
          </div>
          <div class="doc-info">
            <h3 class="doc-name">{{ doc.name }}</h3>
            <div class="doc-meta">
              <span class="doc-size">{{ doc.size }}</span>
              <span class="doc-date">{{ doc.date }}</span>
            </div>
            <div class="doc-footer">
              <div class="author">
                <el-avatar :size="20" :src="doc.authorAvatar" />
                <span class="author-name">{{ doc.author }}</span>
              </div>
              <div class="tags">
                <el-tag v-for="tag in doc.tags" :key="tag" size="small" class="doc-tag">
                  {{ tag }}
                </el-tag>
                <span v-if="doc.extraTags" class="extra-tags">+{{ doc.extraTags }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- List View (Placeholder) -->
      <div v-else class="documents-list">
        <!-- List implementation would go here -->
        <p class="text-center py-20 text-gray-400">列表视图正在开发中。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  Plus, ArrowDown, Moon, Bell, Search, Grid, Expand, Upload, FolderAdd,
  Document, EditPen, Files, DataAnalysis, Picture, PictureFilled, VideoCamera, Box
} from '@element-plus/icons-vue'

const searchQuery = ref('')
const sortBy = ref('date')
const groupBy = ref('none')
const viewType = ref('grid')

const documents = ref([
  {
    id: 1,
    name: '会议记录.txt',
    size: '12.06 KB',
    date: '2024年1月20日',
    type: 'txt',
    author: 'Lucy Pearl',
    authorAvatar: 'https://i.pravatar.cc/100?u=lucy',
    tags: ['笔记', '会议'],
    extraTags: 1
  },
  {
    id: 2,
    name: 'API 接口文档.docx',
    size: '1.18 MB',
    date: '2024年1月19日',
    type: 'docx',
    author: 'Miles Parker',
    authorAvatar: 'https://i.pravatar.cc/100?u=miles',
    tags: ['接口', '文档'],
    extraTags: 1
  },
  {
    id: 3,
    name: '性能报告.pdf',
    size: '1.79 MB',
    date: '2024年1月19日',
    type: 'pdf',
    author: 'Caleb Knox',
    authorAvatar: 'https://i.pravatar.cc/100?u=caleb',
    tags: ['性能', '指标'],
    extraTags: 1
  },
  {
    id: 4,
    name: '第一季度预算分析.xlsx',
    size: '964.51 KB',
    date: '2024年1月18日',
    type: 'xlsx',
    author: 'Maya Lynn',
    authorAvatar: 'https://i.pravatar.cc/100?u=maya',
    tags: ['预算', '分析'],
    extraTags: 1
  },
  {
    id: 5,
    name: '设计系统指南.figma',
    size: '14.95 MB',
    date: '2024年1月17日',
    type: 'figma',
    author: 'Mia Belle',
    authorAvatar: 'https://i.pravatar.cc/100?u=mia',
    tags: ['设计', '指南'],
    extraTags: 1
  },
  {
    id: 6,
    name: '项目需求.pdf',
    size: '2.34 MB',
    date: '2024年1月16日',
    type: 'pdf',
    author: 'Lily Grace',
    authorAvatar: 'https://i.pravatar.cc/100?u=lily',
    tags: ['需求', '规划'],
    extraTags: 1
  },
  {
    id: 7,
    name: '客户反馈.docx',
    size: '554.58 KB',
    date: '2024年1月16日',
    type: 'docx',
    author: 'Owen Scott',
    authorAvatar: 'https://i.pravatar.cc/100?u=owen',
    tags: ['反馈', '客户'],
    extraTags: 1
  },
  {
    id: 8,
    name: '团队演示文稿.pptx',
    size: '5.18 MB',
    date: '2024年1月15日',
    type: 'pptx',
    author: 'Adam Reid',
    authorAvatar: 'https://i.pravatar.cc/100?u=adam',
    tags: ['演示', '团队'],
    extraTags: 1
  },
  {
    id: 9,
    name: 'UI 原型图.png',
    size: '2.24 MB',
    date: '2024年1月14日',
    type: 'png',
    author: 'Mia Belle',
    authorAvatar: 'https://i.pravatar.cc/100?u=mia',
    tags: ['原型', '界面'],
    extraTags: 1
  },
  {
    id: 10,
    name: '用户研究报告.pdf',
    size: '3.3 MB',
    date: '2024年1月12日',
    type: 'pdf',
    author: 'Isla Brooke',
    authorAvatar: 'https://i.pravatar.cc/100?u=isla',
    tags: ['研究', '用户'],
    extraTags: 1
  },
  {
    id: 11,
    name: '产品演示视频.mp4',
    size: '43.56 MB',
    date: '2024年1月9日',
    type: 'mp4',
    author: 'Zoe Jane',
    authorAvatar: 'https://i.pravatar.cc/100?u=zoe',
    tags: ['演示', '视频'],
    extraTags: 1
  },
  {
    id: 12,
    name: '品牌资产.zip',
    size: '22.37 MB',
    date: '2024年1月8日',
    type: 'zip',
    author: 'Lily Grace',
    authorAvatar: 'https://i.pravatar.cc/100?u=lily',
    tags: ['品牌', '资产'],
    extraTags: 1
  }
])

const filteredDocuments = computed(() => {
  if (!searchQuery.value) return documents.value
  const query = searchQuery.value.toLowerCase()
  return documents.value.filter(doc =>
      doc.name.toLowerCase().includes(query) ||
      doc.tags.some(tag => tag.toLowerCase().includes(query)) ||
      doc.author.toLowerCase().includes(query)
  )
})
</script>

<style scoped lang="scss">
.project-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;

  .title-area {
    h1 {
      font-size: 28px;
      font-weight: 700;
      color: #1d1d1f;
      margin-bottom: 8px;
    }
    p {
      color: #86868b;
      font-size: 16px;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 16px;

    .new-project-btn {
      height: 40px;
      border-radius: 8px;
      background-color: #007aff;
      border: none;
      font-weight: 500;
    }

    .top-icons {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #86868b;

      .icon-btn {
        font-size: 20px;
        cursor: pointer;
        &:hover { color: #1d1d1f; }
      }
    }
  }
}

.content-card {
  padding: 24px;
  border-radius: 16px;
  background: white;
  box-shadow: 0 4px 24px rgba(0,0,0,0.04);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;

  .search-box {
    flex: 1;
    max-width: 480px;

    .search-input {
      :deep(.el-input__wrapper) {
        border-radius: 8px;
        background-color: #f5f5f7;
        box-shadow: none !important;
        border: 1px solid transparent;
        &:hover { border-color: #d2d2d7; }
        &.is-focus { border-color: #007aff; background-color: white; }
      }
      .command-hint {
        font-size: 12px;
        color: #86868b;
        background: #e5e5e7;
        padding: 2px 6px;
        border-radius: 4px;
      }
    }
  }

  .filter-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .filter-select {
      width: 130px;
      :deep(.el-input__wrapper) {
        border-radius: 8px;
        background-color: #f5f5f7;
        box-shadow: none !important;
      }
    }

    .view-toggle {
      :deep(.el-button) {
        border-radius: 8px;
        background-color: #f5f5f7;
        border: none;
        &.active {
          background-color: white;
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
          color: #007aff;
        }
      }
    }

    .upload-btn {
      border-radius: 8px;
      background-color: #007aff;
    }

    .new-folder-btn {
      border-radius: 8px;
      border: 1px solid #d2d2d7;
    }
  }
}

.count-info {
  font-size: 14px;
  color: #86868b;
  margin-bottom: 20px;
}

.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.doc-card {
  border: 1px solid #f5f5f7;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  cursor: pointer;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0,0,0,0.08);
    border-color: transparent;
  }

  .doc-icon-wrapper {
    width: 48px;
    height: 48px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    margin-bottom: 16px;
    background-color: #f5f5f7;

    &.txt { color: #86868b; background-color: #f5f5f7; }
    &.docx { color: #007aff; background-color: rgba(0, 122, 255, 0.1); }
    &.pdf { color: #ff3b30; background-color: rgba(255, 59, 48, 0.1); }
    &.xlsx { color: #34c759; background-color: rgba(52, 199, 89, 0.1); }
    &.figma { color: #af52de; background-color: rgba(175, 82, 222, 0.1); }
    &.png { color: #ff9500; background-color: rgba(255, 149, 0, 0.1); }
    &.mp4 { color: #5856d6; background-color: rgba(88, 86, 214, 0.1); }
    &.zip { color: #5ac8fa; background-color: rgba(90, 200, 250, 0.1); }
  }

  .doc-info {
    .doc-name {
      font-size: 16px;
      font-weight: 600;
      color: #1d1d1f;
      margin-bottom: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .doc-meta {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #86868b;
      margin-bottom: 16px;
    }

    .doc-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .author {
        display: flex;
        align-items: center;
        gap: 8px;
        .author-name {
          font-size: 12px;
          color: #1d1d1f;
        }
      }

      .tags {
        display: flex;
        align-items: center;
        gap: 4px;
        .doc-tag {
          border: none;
          background-color: #f5f5f7;
          color: #1d1d1f;
          font-weight: 500;
        }
        .extra-tags {
          font-size: 11px;
          color: #86868b;
          font-weight: 500;
        }
      }
    }
  }
}

.glass-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.mr-2 { margin-right: 8px; }
.ml-2 { margin-left: 8px; }
.pr-2 { padding-right: 8px; }
.pl-2 { padding-left: 8px; }
.border-l { border-left-width: 1px; }
</style>
