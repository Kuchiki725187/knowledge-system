import request from './request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function getMe() {
  return request.get('/user/me')
}

export function updateMe(data) {
  return request.put('/user/me', data)
}

export function refreshToken(data) {
  return request.post('/user/refresh', data)
}
