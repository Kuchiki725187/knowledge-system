import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: localStorage.getItem('access_token') || '',
    refreshToken: localStorage.getItem('refresh_token') || '',
    userInfo: null,
  }),
  actions: {
    setTokens(access, refresh) {
      this.accessToken = access
      this.refreshToken = refresh
      localStorage.setItem('access_token', access)
      localStorage.setItem('refresh_token', refresh)
    },
    setUserInfo(info) {
      this.userInfo = info
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
    },
  },
})
