import request from '@/utils/request'

export function listDepartmentLibraries() {
  return request({ url: '/mes/ai/kb/libraries', method: 'get' })
}

export function listAdminDepartmentLibraries() {
  return request({ url: '/mes/ai/kb/libraries/admin', method: 'get' })
}

export function createDepartmentLibrary(data) {
  return request({ url: '/mes/ai/kb/libraries', method: 'post', data })
}

function documentsUrl(libraryId) {
  return `/mes/ai/kb/libraries/${libraryId}/documents`
}

export function listLibraryDocuments(libraryId) {
  return request({ url: documentsUrl(libraryId), method: 'get' })
}

export function uploadLibraryDocument(libraryId, data) {
  return request({
    url: documentsUrl(libraryId),
    method: 'post',
    data,
    timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  })
}

export function previewLibraryDocument(libraryId, versionId, page) {
  return request({ url: `${documentsUrl(libraryId)}/versions/${versionId}/preview`, method: 'get', params: { page } })
}

export function downloadLibraryOriginal(libraryId, versionId) {
  return request({ url: `${documentsUrl(libraryId)}/versions/${versionId}/original`, method: 'get', responseType: 'blob', timeout: 120000 })
}

export function retryLibraryDocument(libraryId, versionId) {
  return request({ url: `${documentsUrl(libraryId)}/versions/${versionId}/retry`, method: 'post' })
}

export function deleteLibraryDocument(libraryId, docId) {
  return request({ url: `${documentsUrl(libraryId)}/${docId}`, method: 'delete' })
}

export function getLibraryGraph(libraryId, params) {
  return request({ url: `/mes/ai/kb/libraries/${libraryId}/graph`, method: 'get', params })
}

export function retryLibraryGraph(libraryId, versionId) {
  return request({ url: `/mes/ai/kb/libraries/${libraryId}/graph/versions/${versionId}/retry`, method: 'post' })
}

/** 物料影响路径：按完整编码反向查出子件被哪些上级产品使用。 */
export function getImpactPath(libraryId, itemCode) {
  return request({ url: `/mes/ai/kb/libraries/${libraryId}/graph/impact`, method: 'get', params: { itemCode } })
}

/** 某一段的 BOM 行与文档佐证；via 为截至该段的有序行号。 */
export function getImpactEdge(libraryId, bomId, params) {
  return request({ url: `/mes/ai/kb/libraries/${libraryId}/graph/impact/edges/${bomId}`, method: 'get', params })
}
