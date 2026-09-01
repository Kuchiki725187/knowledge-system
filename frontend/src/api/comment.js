import request from './request'

export function addComment(knowledgeId, data) {
  return request.post(`/comment/${knowledgeId}`, data)
}

export function pageComment(knowledgeId, params) {
  return request.get(`/comment/${knowledgeId}/page`, { params })
}

export function deleteComment(commentId) {
  return request.delete(`/comment/${commentId}`)
}
