import request from '@/utils/request'

const prefix = '/api/lake/storage/storageToken'

export function listTokens(params) {
  // params: { pageNum, pageSize, keyword }
  return request({
    url: `${prefix}/datatables`,
    method: 'post',
    params: params
  })
}

export function getToken(id) {
  return request({
    url: `${prefix}/${id}`,
    method: 'get'
  })
}

export function addToken(data) {
  return request({
    url: prefix,
    method: 'post',
    data: data
  })
}

export function updateToken(data) {
  return request({
    url: prefix,
    method: 'put',
    data: data
  })
}

export function deleteToken(ids) {
  // ids: string, comma-separated or single id
  return request({
    url: `${prefix}/${ids}`,
    method: 'delete'
  })
}

export function generateTokenApi() {
  return request({
    url: `${prefix}/generate`,
    method: 'post'
  })
}