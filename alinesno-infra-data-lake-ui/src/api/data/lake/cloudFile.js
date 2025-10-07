import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/**
 * 云文件接口操作
 * 
 * @author luoxiaodong
 * @since 1.0.0
 */

// 接口配置项
var prefix = '/api/infra/data/lake/storage/cloudFile/';

// 文件统计 
export function statistics(folderId) {
  return request({
    url: prefix + 'statistics?folderId=' + parseStrEmpty(folderId),
    method: 'get'
  })
}

// 创建文件夹
export function createFolder(data) {
  return request({
    url: prefix + 'folder',
    method: 'post',
    data: data
  })
}

// 删除文件夹
export function deleteFolder(id) {
  return request({
    url: prefix + 'deleteFolder',
    method: 'delete',
    params: { id: id }
  })
}

// 获取文件列表（使用现有的datatables接口）
export function listFiles(query) {
  return request({
    url: prefix + 'datatables' ,
    method: 'post',
    params: query
  })
}

// 删除文件
export function deleteFile(id) {
  return request({
    url: prefix + 'deleteFile',
    method: 'delete',
    params: { id: id }
  })
}

// 上传o业务域的文件
export function catalogUploadFile(data, config = {}) {
  return request({
    url: prefix + 'catalogUploadFile',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: config.onUploadProgress || (() => {}),
    timeout: 0 // 取消超时限制，适合大文件上传
  })
}


// 上传文件
export function uploadFile(data, config = {}) {
  return request({
    url: prefix + 'upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: config.onUploadProgress || (() => {}),
    timeout: 0 // 取消超时限制，适合大文件上传
  })
}

// 获取文件夹树
export function getFolderTree() {
  return request({
    url: prefix + 'folderTree',
    method: 'get'
  })
}