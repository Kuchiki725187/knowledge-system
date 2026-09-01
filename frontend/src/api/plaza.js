import request from './request'

export function pagePlaza(params) {
  return request.get('/plaza/page', { params })
}
