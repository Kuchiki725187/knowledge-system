<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pagePlaza } from '../api/plaza'

const router = useRouter()
const loading = ref(false)
const total = ref(0)
const records = ref([])
const query = reactive({ page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const data = await pagePlaza(query)
    records.value = data.records
    total.value = data.total
  } catch (e) {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div v-if="records.length" class="grid">
      <el-card v-for="item in records" :key="item.id" shadow="hover" class="card"
        @click="router.push(`/knowledge/${item.id}`)">
        <div class="title">{{ item.title }}</div>
        <div class="summary">{{ item.summary || '作者很懒，没有写摘要' }}</div>
        <div class="footer">
          <span class="author">{{ item.authorName }}</span>
          <span class="stats">
            浏览 {{ item.viewCount }} · 赞 {{ item.likeCount }} · 藏 {{ item.favoriteCount }}
          </span>
          <span class="time">{{ item.updateTime }}</span>
        </div>
      </el-card>
    </div>
    <el-empty v-if="!loading && records.length === 0" description="大厅还没有人发布知识" />

    <div class="pager">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
        layout="total, prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.card {
  cursor: pointer;
}
.title {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 8px;
}
.summary {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 44px;
}
.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
.author {
  color: #409eff;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
