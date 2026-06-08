import axios from 'axios'

export const API_BASE_URL = 'http://localhost:8123/api'

export const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
})

export function createSseUrl(path, params = {}) {
  const url = new URL(`${API_BASE_URL}${path}`)

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value)
    }
  })

  return url.toString()
}
