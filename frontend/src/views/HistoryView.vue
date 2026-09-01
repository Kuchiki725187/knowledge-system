<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { recentHistory } from '../api/browse'

const router = useRouter()
const loading = ref(false)
const records = ref([])

onMounted(async () => {
  loading.value = true
  try {
    records.value = await recentHistory(20)
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <el-table v-loading="loading" :data="records">
      <el-table-column label="标题" min-width="260">
        <template #default="{ row }">
          <span v-if="row.deleted" class="deleted-text">该知识已删除</span>
          <el-link v-else type="primary" @click="router.push(`/knowledge/${row.id}`)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="140" />
      <el-table-column prop="browseTime" label="浏览时间" width="180" />
    </el-table>
  </div>
</template>

<style scoped>
.deleted-text {
  color: #c0c4cc;
  text-decoration: line-through;
}
</style>
