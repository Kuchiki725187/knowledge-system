<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageFavorite, removeFavorite } from '../api/favorite'

const router = useRouter()
const loading = ref(false)
const total = ref(0)
const records = ref([])
const query = reactive({ page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const data = await pageFavorite(query)
    records.value = data.records
    total.value = data.total
  } catch (e) {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleRemove(row) {
  await removeFavorite(row.id)
  ElMessage.success('已取消收藏')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <el-table v-loading="loading" :data="records">
      <el-table-column label="标题" min-width="240">
        <template #default="{ row }">
          <span v-if="row.deleted" class="deleted-text">该知识已删除</span>
          <el-link v-else type="primary" @click="router.push(`/knowledge/${row.id}`)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="favoriteTime" label="收藏时间" width="170" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="warning" @click="handleRemove(row)">取消收藏</el-button>
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
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.deleted-text {
  color: #c0c4cc;
  text-decoration: line-through;
}
</style>
