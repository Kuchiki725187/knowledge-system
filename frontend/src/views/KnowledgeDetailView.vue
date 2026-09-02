<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getKnowledge, deleteKnowledge } from '../api/knowledge'
import { addFavorite, removeFavorite, pageFavorite } from '../api/favorite'
import { addLike, removeLike } from '../api/like'
import { addComment, pageComment, deleteComment } from '../api/comment'
import { uploadAttachment, listAttachments, deleteAttachment, downloadAttachmentFile } from '../api/attachment'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const detail = ref(null)
const favorited = ref(false)

// 是否自己的知识:是则显示编辑/删除,否则只能看和收藏
const isOwner = computed(() =>
  detail.value && store.userInfo && detail.value.authorId === store.userInfo.id
)

async function load() {
  detail.value = await getKnowledge(route.params.id)
}

async function loadFavorited() {
  try {
    const page = await pageFavorite({ page: 1, size: 100 })
    favorited.value = (page.records || []).some((f) => f.id === Number(route.params.id))
  } catch (e) { /* 静默 */ }
}

async function toggleFavorite() {
  if (favorited.value) {
    await removeFavorite(detail.value.id)
    favorited.value = false
    ElMessage.success('已取消收藏')
  } else {
    await addFavorite(detail.value.id)
    favorited.value = true
    ElMessage.success('已收藏')
  }
}

// 点赞/取消:本地同步更新计数,不重新拉详情
const likeLoading = ref(false)
async function toggleLike() {
  if (likeLoading.value) return
  likeLoading.value = true
  try {
    if (detail.value.isLiked) {
      await removeLike(detail.value.id)
      detail.value.isLiked = false
      detail.value.likeCount--
    } else {
      await addLike(detail.value.id)
      detail.value.isLiked = true
      detail.value.likeCount++
    }
  } finally {
    likeLoading.value = false
  }
}

async function handleDelete() {
  await ElMessageBox.confirm(`确定删除「${detail.value.title}」？`, '提示', { type: 'warning' })
  await deleteKnowledge(detail.value.id)
  ElMessage.success('已删除')
  router.push('/knowledge')
}

// ---- 评论区 ----
const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentContent = ref('')
const commentLoading = ref(false)
const submitting = ref(false)

async function loadComments() {
  commentLoading.value = true
  try {
    const page = await pageComment(route.params.id, { page: commentPage.value, size: 10 })
    comments.value = page.records || []
    commentTotal.value = page.total
  } finally {
    commentLoading.value = false
  }
}

async function submitComment() {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    await addComment(route.params.id, { content: commentContent.value })
    commentContent.value = ''
    commentPage.value = 1
    await loadComments()
    detail.value.commentCount++
    ElMessage.success('评论成功')
  } finally {
    submitting.value = false
  }
}

async function handleDeleteComment(id) {
  await ElMessageBox.confirm('确定删除这条评论？', '提示', { type: 'warning' })
  await deleteComment(id)
  await loadComments()
  detail.value.commentCount--
  ElMessage.success('已删除')
}

// ---- 附件区 ----
const attachments = ref([])
const uploading = ref(false)

async function loadAttachments() {
  try {
    attachments.value = await listAttachments(route.params.id)
  } catch (e) { /* 静默 */ }
}

// el-upload 自定义上传:走 axios(自动带 token),成功后再刷新列表
async function doUpload(options) {
  uploading.value = true
  try {
    await uploadAttachment(route.params.id, options.file)
    ElMessage.success('上传成功')
    options.onSuccess()
    await loadAttachments()
  } catch (e) {
    options.onError(e)
  } finally {
    uploading.value = false
  }
}

// 下载:blob 从后端拉回来,用 a 标签触发浏览器保存
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

async function handleDeleteAttachment(id) {
  await ElMessageBox.confirm('确定删除该附件？', '提示', { type: 'warning' })
  await deleteAttachment(id)
  ElMessage.success('已删除')
  await loadAttachments()
}

function formatSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

onMounted(async () => {
  await load()
  loadFavorited()
  loadComments()
  loadAttachments()
})
</script>

<template>
  <div v-if="detail" class="detail">
    <div class="head">
      <h1>{{ detail.title }}</h1>
      <div class="actions">
        <el-button v-if="isOwner" size="small" @click="router.push(`/knowledge/${detail.id}/edit`)">编辑</el-button>
        <el-button size="small" :type="favorited ? 'warning' : 'primary'" @click="toggleFavorite">
          {{ favorited ? '取消收藏' : '收藏' }}
        </el-button>
        <el-button v-if="isOwner" size="small" type="danger" @click="handleDelete">删除</el-button>
      </div>
    </div>
    <div class="meta">
      <el-tag type="primary" size="small">{{ detail.authorName }}</el-tag>
      <el-tag v-if="detail.categoryName" type="success" size="small">{{ detail.categoryName }}</el-tag>
      <el-tag v-for="t in detail.tags" :key="t.id" type="info" size="small">{{ t.name }}</el-tag>
      <span class="info">浏览 {{ detail.viewCount }} · 更新于 {{ detail.updateTime }}</span>
    </div>
    <MdPreview :model-value="detail.content" />

    <!-- 互动栏 -->
    <div class="interact">
      <el-button
        :type="detail.isLiked ? 'danger' : 'default'"
        :loading="likeLoading"
        round
        @click="toggleLike"
      >
        {{ detail.isLiked ? '已赞' : '点赞' }} {{ detail.likeCount }}
      </el-button>
      <el-button round disabled>评论 {{ detail.commentCount }}</el-button>
    </div>

    <!-- 附件区 -->
    <div class="attachments">
      <div class="attachments-head">
        <h3>附件 ({{ attachments.length }})</h3>
        <el-upload
          v-if="isOwner"
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
          <el-button v-if="isOwner" link type="danger" size="small" @click="handleDeleteAttachment(a.id)">
            删除
          </el-button>
        </div>
      </div>
      <el-empty v-else description="暂无附件" :image-size="50" />
    </div>

    <!-- 评论区 -->
    <div class="comments">
      <h3>评论 ({{ detail.commentCount }})</h3>
      <div class="comment-editor">
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="写下你的评论..."
        />
        <el-button type="primary" :loading="submitting" style="margin-top: 8px" @click="submitComment">
          发表评论
        </el-button>
      </div>
      <div v-loading="commentLoading" class="comment-list">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-head">
            <span class="comment-author">{{ c.authorName }}</span>
            <span class="comment-time">{{ c.createTime }}</span>
            <el-button
              v-if="c.userId === store.userInfo?.id"
              link
              type="danger"
              size="small"
              @click="handleDeleteComment(c.id)"
            >
              删除
            </el-button>
          </div>
          <div class="comment-content">{{ c.content }}</div>
        </div>
        <el-empty v-if="!commentLoading && comments.length === 0" description="还没有评论,来抢沙发" :image-size="60" />
      </div>
      <el-pagination
        v-if="commentTotal > 10"
        v-model:current-page="commentPage"
        :total="commentTotal"
        :page-size="10"
        layout="prev, pager, next"
        style="justify-content: center; margin-top: 12px"
        @current-change="loadComments"
      />
    </div>
  </div>
</template>

<style scoped>
.detail {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 12px 0 20px;
  flex-wrap: wrap;
}
.info {
  font-size: 13px;
  color: #909399;
}
.interact {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 20px 0;
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.attachments {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
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
.comments {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}
.comment-editor {
  margin: 12px 0 20px;
}
.comment-item {
  padding: 12px 0;
  border-bottom: 1px dashed #eee;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.comment-author {
  font-weight: 600;
  font-size: 14px;
}
.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}
.comment-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
