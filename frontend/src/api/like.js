import request from './request'

export function addLike(knowledgeId) {
  return request.post(`/like/${knowledgeId}`)
}

export function removeLike(knowledgeId) {
  return request.delete(`/like/${knowledgeId}`)
}
