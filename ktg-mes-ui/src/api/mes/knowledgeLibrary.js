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
