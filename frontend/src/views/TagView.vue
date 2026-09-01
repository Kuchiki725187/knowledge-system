<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTag, createTag, deleteTag } from '../api/tag'

const loading = ref(false)
const records = ref([])
const dialogVisible = ref(false)
const form = reactive({ name: '' })

async function load() {
  loading.value = true
  try {
    records.value = await listTag()
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  await createTag({ name: form.name })
  ElMessage.success('已创建')
  dialogVisible.value = false
  form.name = ''
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除标签「${row.name}」？关联会一并清理。`, '提示', { type: 'warning' })
  await deleteTag(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="dialogVisible = true">新建标签</el-button>
    </div>
    <el-table v-loading="loading" :data="records">
      <el-table-column prop="name" label="标签名称" min-width="180" />
      <el-table-column prop="usageCount" label="使用次数" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建标签" width="380px">
      <el-input v-model="form.name" maxlength="32" placeholder="标签名称" @keyup.enter="handleCreate" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
