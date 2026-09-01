<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchKnowledge } from '../api/search'

const router = useRouter()
const loading = ref(false)
const searched = ref(false)
const total = ref(0)
const records = ref([])
const query = reactive({ keyword: '', page: 1, size: 10 })

async function doSearch() {
  if (!query.keyword.trim()) return
  loading.value = true
  searched.value = true
  try {
    const data = await searchKnowledge(query)
    records.value = data.records
    total.value = data.total
  } catch (e) {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange() {
  doSearch()
}
</script>

<template>
  <div>
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索我的知识库" size="large" clearable
        @keyup.enter="doSearch" @clear="searched = false; records = []; total = 0">
        <template #append>
          <el-button @click="doSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-table v-if="searched" v-loading="loading" :data="records">
      <el-table-column label="标题" min-width="260">
        <template #default="{ row }">
          <el-link type="primary" @click="router.push(`/knowledge/${row.id}`)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
    </el-table>
    <el-empty v-if="searched && !loading && records.length === 0" description="没有找到相关内容" />

    <div class="pager" v-if="total > query.size">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
        layout="total, prev, pager, next" @current-change="handlePageChange" />
    </div>
  </div>
</template>

<style scoped>
.search-bar {
  margin-bottom: 20px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
