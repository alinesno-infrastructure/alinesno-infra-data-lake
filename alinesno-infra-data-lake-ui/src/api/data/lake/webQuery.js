import request from '@/utils/request'

/**
 * Web SQL 查询相关接口
 *
 * 后端控制器: com.alinesno.infra.data.lake.api.controller.WebQueryController
 * POST /api/infra/data/lake/webQuery        -> 执行查询，body: { sql, page, pageSize, tableContext }
 * POST /api/infra/data/lake/webQuery/explain -> Explain，body: { sql, tableContext }
 */

// 基础前缀（与后端 @RequestMapping 保持一致）
const prefix = '/api/infra/data/lake/webQuery'

/**
 * 执行 SQL 查询（轻量封装）
 * @param {Object} payload - 请求体, e.g. { sql, page, pageSize, tableContext }
 * @returns {Promise} axios Promise
 */
export function query(payload) {
  return request({
    url: prefix,
    method: 'post',
    data: payload
  })
}

/**
 * Explain SQL（返回文本）
 * @param {Object} payload - 请求体, e.g. { sql, tableContext }
 * @returns {Promise} axios Promise (text)
 */
export function explain(payload) {
  return request({
    url: `${prefix}/explain`,
    method: 'post',
    data: payload,
    // 如果 request util 需要特别处理 text 响应，可在 util 中处理，通常不需要在这里设置
  })
}