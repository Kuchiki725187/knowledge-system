<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCategory, createCategory, updateCategory, deleteCategory } from '../api/category'

const loading = ref(false)
const records = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', sort: 0 })

async function load() {
  loading.value = true
  try {
    records.value = await listCategory()
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.sort = 0
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name
  form.sort = row.sort
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (editingId.value) {
    await updateCategory(editingId.value, { name: form.name, sort: form.sort })
    ElMessage.success('已更新')
  } else {
    await createCategory({ name: form.name, sort: form.sort })
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建分类</el-button>
    </div>
    <el-table v-loading="loading" :data="records">
      <el-table-column prop="name" label="分类名称" min-width="180" />
      <el-table-column prop="knowledgeCount" label="知识数" width="100" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新建分类'" width="420px">
      <el-form label-width="70px">
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
