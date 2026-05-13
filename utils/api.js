// API 基础地址，通过 Postman 环境变量切换 mock/dev/prod
const BASE_URL = 'http://192.168.1.10:9482'

// 通用 GET 请求封装
const get = (path, params) => {
  return new Promise((resolve, reject) => {
    const query = Object.keys(params)
      .filter(k => params[k] !== undefined && params[k] !== null)
      .map(k => `${k}=${encodeURIComponent(params[k])}`)
      .join('&')
    const url = `${BASE_URL}${path}${query ? '?' + query : ''}`

    wx.request({
      url,
      method: 'GET',
      success(res) {
        if (res.statusCode === 200 && res.data && res.data.code === 0) {
          resolve(res.data.data)
        } else {
          reject(res.data || { code: -1, message: '请求失败' })
        }
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

// 获取赛事列表
const getTournaments = (params = {}) => {
  return get('/api/fantastic/query/tournaments', params)
}

// 获取比赛列表（返回 {liveMatches, upcomingMatches, finishedMatches}）
const getMatches = (params = {}) => {
  return get('/api/fantastic/query/matches', params)
}

// 获取球员列表（tour: 'atp' | 'wta'）
const getPlayers = (tour) => {
  return get('/api/fantastic/query/players', { tour })
}

module.exports = {
  getTournaments,
  getMatches,
  getPlayers
}
