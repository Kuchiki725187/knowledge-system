import request from './request'

export function getOverview() {
  return request.get('/stats/overview')
}

export function getTrend(days = 7) {
  return request.get('/stats/trend', { params: { days } })
}

export function getHot(limit = 10) {
  return request.get('/stats/hot', { params: { limit } })
}
