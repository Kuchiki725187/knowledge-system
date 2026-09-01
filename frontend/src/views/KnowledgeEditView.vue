<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createKnowledge, updateKnowledge, getKnowledge } from '../api/knowledge'
import { listCategory } from '../api/category'
import { listTag } from '../api/tag'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const categories = ref([])
const tags = ref([])
const saving = ref(false)
const form = reactive({ title: '', content: '', categoryId: null, tags: [], status: 0 })

onMounted(async () => {
  try {
    categories.value = await listCategory()
    tags.value = await listTag()
  } catch (e) { /* 接口未实现时静默 */ }
  if (id.value) {
    const d = await getKnowledge(id.value)
    form.title = d.title
    form.content = d.content
    form.categoryId = d.categoryId
    form.tags = (d.tags || []).map((t) => t.name)
    form.status = d.status
  }
})

async function handleSave() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入正文')
    return
  }
  saving.value = true
  try {
    if (id.value) {
      await updateKnowledge(id.value, form)
      ElMessage.success('已保存')
      router.push(`/knowledge/${id.value}`)
    } else {
      const d = await createKnowledge(form)
      ElMessage.success('已创建')
      router.push(`/knowledge/${d.id}`)
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <el-input v-model="form.title" placeholder="标题" size="large" maxlength="128" show-word-limit />
    <div class="meta">
      <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 180px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="form.tags" multiple filterable allow-create default-first-option
        placeholder="标签（可输入新建）" style="width: 320px">
        <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.name" />
      </el-select>
      <el-radio-group v-model="form.status">
        <el-radio :value="0">草稿</el-radio>
        <el-radio :value="1">发布</el-radio>
      </el-radio-group>
      <div class="spacer" />
      <el-button @click="router.back()">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>
    <MdEditor v-model="form.content" style="height: 520px" />
  </div>
</template>

<style scoped>
.meta {
  display: flex;
  gap: 12px;
  margin: 12px 0;
  align-items: center;
}
.spacer {
  flex: 1;
}
</style>
