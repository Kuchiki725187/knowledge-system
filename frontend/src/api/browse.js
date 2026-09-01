import request from './request'

export function recentHistory(limit = 20) {
  return request.get('/browse-history/recent', { params: { limit } })
}
