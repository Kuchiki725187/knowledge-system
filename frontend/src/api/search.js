import request from './request'

export function searchKnowledge(params) {
  return request.get('/search', { params })
}
