import axios from 'axios'
import request from './request'

export function uploadAttachment(knowledgeId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/attachment/${knowledgeId}`, formData)
}

export function listAttachments(knowledgeId) {
  return request.get(`/attachment/list/${knowledgeId}`)
}

export function deleteAttachment(attachmentId) {
  return request.delete(`/attachment/${attachmentId}`)
}

// 下载:blob 响应没有 Result 结构,不能走 request 拦截器(会误判失败),
// 用原始 axios + 手动带 token
export function downloadAttachmentFile(attachmentId) {
  return axios.get(`/api/attachment/download/${attachmentId}`, {
    responseType: 'blob',
    headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
  })
}
