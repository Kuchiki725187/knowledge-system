<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOverview, getTrend, getHot } from '../api/stats'

const router = useRouter()
const overview = ref({ knowledgeCount: 0, categoryCount: 0, tagCount: 0, favoriteCount: 0 })
const trend = ref([])
const hot = ref([])
const trendDays = ref(7)

async function loadTrend() {
  try {
    trend.value = await getTrend(trendDays.value)
  } catch (e) {
    trend.value = []
  }
}

onMounted(async () => {
  try {
    overview.value = await getOverview()
    hot.value = await getHot(10)
    await loadTrend()
  } catch (e) { /* 接口未实现时静默 */ }
})
</script>

<template>
  <div>
    <div class="cards">
      <el-card shadow="hover">
        <div class="num">{{ overview.knowledgeCount }}</div>
        <div class="label">知识总数</div>
      </el-card>
      <el-card shadow="hover">
        <div class="num">{{ overview.categoryCount }}</div>
        <div class="label">分类数</div>
      </el-card>
      <el-card shadow="hover">
        <div class="num">{{ overview.tagCount }}</div>
        <div class="label">标签数</div>
      </el-card>
      <el-card shadow="hover">
        <div class="num">{{ overview.favoriteCount }}</div>
        <div class="label">收藏数</div>
      </el-card>
    </div>

    <el-card shadow="never" class="section">
      <template #header>
        <div class="section-head">
          <span>近 N 天新增知识</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="trend" size="small">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="count" label="新增知识数" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="section">
      <template #header>浏览量 Top 10</template>
      <el-table :data="hot" size="small">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column label="标题">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/knowledge/${row.id}`)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.num {
  font-size: 28px;
  font-weight: 700;
}
.label {
  margin-top: 4px;
  color: #909399;
  font-size: 14px;
}
.section {
  margin-bottom: 16px;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
