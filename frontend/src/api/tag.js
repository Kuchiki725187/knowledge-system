import request from './request'

export function listTag() {
  return request.get('/tag/list')
}

export function createTag(data) {
  return request.post('/tag', data)
}

export function deleteTag(id) {
  return request.delete(`/tag/${id}`)
}
