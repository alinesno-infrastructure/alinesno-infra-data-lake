import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/**
 * 业务域分类操作接口
 * 
 * @author luoxiaodong
 * @since 1.0.0
 */

// 接口配置项
const prefix = '/api/infra/data/lake/domain';

// 查询域分类列表
export function listDomains(query) {
  return request({
    url: `${prefix}/datatables`,
    method: 'post',
    params: query
  })
}

// 获取所有启用的域分类
export function listEnabledDomains() {
  return request({
    url: `${prefix}/enabled`,
    method: 'get'
  })
}

// 根据ID获取域分类详情
export function getDomain(id) {
  return request({
    url: `${prefix}/${parseStrEmpty(id)}`,
    method: 'get'
  })
}

// 新增域分类
export function addDomain(data) {
  return request({
    url: `${prefix}`,
    method: 'post',
    data: data
  })
}

// 修改域分类
export function updateDomain(data) {
  return request({
    url: `${prefix}`,
    method: 'put',
    data: data
  })
}

// 删除域分类
export function delDomain(id) {
  return request({
    url: `${prefix}/${id}`,
    method: 'delete'
  })
}

// 检查名称是否已存在
export function checkNameExist(name, excludeId = null) {
  return request({
    url: `${prefix}/checkName`,
    method: 'get',
    params: { 
      name: name,
      excludeId: excludeId
    }
  })
}