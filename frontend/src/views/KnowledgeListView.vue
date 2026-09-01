<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageKnowledge, deleteKnowledge } from '../api/knowledge'
import { listCategory } from '../api/category'

const router = useRouter()
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const records = ref([])
const query = reactive({
  page: 1,
  size: 10,
  categoryId: null,
  status: null,
  keyword: '',
  orderBy: 'update_time',
})

async function loadCategories() {
  try {
    categories.value = await listCategory()
  } catch (e) { /* 接口未实现时静默 */ }
}

async function load() {
  loading.value = true
  try {
    const data = await pageKnowledge(query)
    records.value = data.records
    total.value = data.total
  } catch (e) {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.title}」？`, '提示', { type: 'warning' })
  await deleteKnowledge(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => {
  loadCategories()
  load()
})
</script>

<template>
  <div>
    <div class="toolbar">
      <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 140px" @change="search">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="search">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
      </el-select>
      <el-select v-model="query.orderBy" style="width: 130px" @change="search">
        <el-option label="最近更新" value="update_time" />
        <el-option label="最近创建" value="create_time" />
        <el-option label="浏览最多" value="view_count" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="标题关键词" clearable style="width: 200px"
        @keyup.enter="search" @clear="search" />
      <el-button type="primary" @click="search">查询</el-button>
      <div class="spacer" />
      <el-button type="primary" @click="router.push('/knowledge/new')">新建知识</el-button>
    </div>

    <el-table v-loading="loading" :data="records">
      <el-table-column prop="title" label="标题" min-width="220">
        <template #default="{ row }">
          <el-link type="primary" @click="router.push(`/knowledge/${row.id}`)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/knowledge/${row.id}/edit`)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
        layout="total, prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.spacer {
  flex: 1;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
