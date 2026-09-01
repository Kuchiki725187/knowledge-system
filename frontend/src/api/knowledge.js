import request from './request'

export function createKnowledge(data) {
  return request.post('/knowledge', data)
}

export function updateKnowledge(id, data) {
  return request.put(`/knowledge/${id}`, data)
}

export function deleteKnowledge(id) {
  return request.delete(`/knowledge/${id}`)
}

export function getKnowledge(id) {
  return request.get(`/knowledge/${id}`)
}

export function pageKnowledge(params) {
  return request.get('/knowledge/page', { params })
}

export function getRelations(id) {
  return request.get(`/knowledge/${id}/relations`)
}

export function addRelation(id, data) {
  return request.post(`/knowledge/${id}/relations`, data)
}

export function deleteRelation(relationId) {
  return request.delete(`/knowledge/relations/${relationId}`)
}
