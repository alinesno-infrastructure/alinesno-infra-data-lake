import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/**
 * 数据目录管理 
 * 
 * @author luoxiaodong
 * @since 1.0.0
 */

// 接口配置项
var prefix = '/api/infra/data/lake/catalog'


// 列出最近的目录
export function latestCatalogs() {
  return request({
    url: prefix + '/latestCatalogs?count=6',
    method: 'get'
  })
}

// 列出所有的域
export function listAllCatalog() {
  return request({
    url: prefix + '/listAllCatalog',
    method: 'get'
  })
}

// 创建数据目录
export function createCatalog(data) {
  return request({
    url: prefix + '/createCatalog',
    method: 'post',
    data: data
  })
}

// 获取目录列表
export function listCatalog(params) {
  return request({
    url: prefix + '/datatables',
    method: 'post',
    params: params
  })
}

// 获取目录详情
export function getCatalog(id) {
  return request({
    url: prefix + '/getCatalogById/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 更新目录
export function updateCatalog(data) {
  return request({
    url: prefix + '/updateCatalog',
    method: 'put',
    data: data
  })
}

// 删除目录
export function deleteCatalog(id) {
  return request({
    url: prefix + '/deleteCatalog/' + parseStrEmpty(id),
    method: 'delete'
  })
}

// 统计目录
export function countStats() {
  return request({
    url: prefix + '/countStats',
    method: 'get'
  })
}