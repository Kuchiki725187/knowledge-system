import request from './request'

export function addFavorite(knowledgeId) {
  return request.post(`/favorite/${knowledgeId}`)
}

export function removeFavorite(knowledgeId) {
  return request.delete(`/favorite/${knowledgeId}`)
}

export function pageFavorite(params) {
  return request.get('/favorite/page', { params })
}
