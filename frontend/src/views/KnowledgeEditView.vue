<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createKnowledge, updateKnowledge, getKnowledge } from '../api/knowledge'
import { listCategory } from '../api/category'
import { listTag } from '../api/tag'
import { uploadAttachment, listAttachments, deleteAttachment, downloadAttachmentFile } from '../api/attachment'

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
  loadAttachments()
})

// ---- 附件区(仅编辑已有知识时可用,创建时还没有知识 id) ----
const attachments = ref([])
const uploading = ref(false)

async function loadAttachments() {
  if (!id.value) return
  try {
    attachments.value = await listAttachments(id.value)
  } catch (e) { /* 静默 */ }
}

async function doUpload(options) {
  uploading.value = true
  try {
    await uploadAttachment(id.value, options.file)
    ElMessage.success('上传成功')
    options.onSuccess()
    await loadAttachments()
  } catch (e) {
    options.onError(e)
  } finally {
    uploading.value = false
  }
}

async function downloadAttachment(a) {
  try {
    const res = await downloadAttachmentFile(a.id)
    const url = URL.createObjectURL(res.data)
    const link = document.createElement('a')
    link.href = url
    link.download = a.fileName
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

async function handleDeleteAttachment(attachmentId) {
  await ElMessageBox.confirm('确定删除该附件？', '提示', { type: 'warning' })
  await deleteAttachment(attachmentId)
  ElMessage.success('已删除')
  await loadAttachments()
}

function formatSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

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

    <!-- 附件区:创建模式没有知识 id,先提示保存;编辑模式可上传/下载/删除 -->
    <div v-if="id" class="attachments">
      <div class="attachments-head">
        <h3>附件 ({{ attachments.length }})</h3>
        <el-upload
          :show-file-list="false"
          :http-request="doUpload"
          accept=".md,.txt,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg,.gif,.webp,.zip,.rar,.7z,.csv,.json,.sql"
        >
          <el-button size="small" type="primary" :loading="uploading">上传附件</el-button>
        </el-upload>
      </div>
      <div v-if="attachments.length" class="attachment-list">
        <div v-for="a in attachments" :key="a.id" class="attachment-item">
          <span class="attachment-name" @click="downloadAttachment(a)">{{ a.fileName }}</span>
          <span class="attachment-meta">{{ formatSize(a.fileSize) }} · {{ a.createTime }}</span>
          <el-button link type="danger" size="small" @click="handleDeleteAttachment(a.id)">删除</el-button>
        </div>
      </div>
      <el-empty v-else description="暂无附件" :image-size="50" />
    </div>
    <el-alert v-else type="info" :closable="false" show-icon title="保存后可在编辑页上传附件" />
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
.attachments {
  margin-top: 16px;
  border-top: 1px solid #eee;
  padding-top: 16px;
}
.attachments-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.attachment-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
}
.attachment-name {
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
}
.attachment-name:hover {
  text-decoration: underline;
}
.attachment-meta {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
