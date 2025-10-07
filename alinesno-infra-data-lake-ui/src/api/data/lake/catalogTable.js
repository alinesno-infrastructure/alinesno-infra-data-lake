import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/**
 *  数据管理表理 
 * 
 * @author luoxiaodong
 * @since 1.0.0
 */

// 接口配置项
var prefix = '/api/infra/data/lake/catalogTable'

export function updateCatalogTable(payload) {
  // 根据后端实际接口调整 URL 与方法（PUT/PATCH/POST）
  return request({
    url: `${prefix}/updateCatalogTableDesc`,
    method: 'post',
    data: payload
  });
}

// 同步数据湖表结构
export function syncTableStructure(catalogId){
  return request({
    url: `${prefix}/syncTableStructure?catalogId=${catalogId}`,
    method: 'get'
  })
}

export function getCatalogTree(catalogId) {
  return request({
    url: `${prefix}/tree`,
    method: 'get',
    params: {
      catalogId
    }
  })
}

// 查询目录
export function findById(id) {
  return request({
    url: `${prefix}/findById/${id}`,
    method: 'get'
  })
}

// 查询
export function getDatatables(params) {
  return request({
    url: `${prefix}/datatables`,
    method: 'post',
    params: params 
  })
}

// 创建表
export function createCatalogTable(data) {
  return request({
    url: prefix + '/createTable',
    method: 'post',
    data: data
  })
}

// 创建目录
export function createCatalogDirectory(data) {
  return request({
    url: prefix + '/createDirectory',
    method: 'post',
    data: data
  })
}