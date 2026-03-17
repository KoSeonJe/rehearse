const DB_NAME = 'rehearse-video-storage'
const DB_VERSION = 1
const STORE_NAME = 'videos'

const openDB = (): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME)
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

// 키 구조: interviewId-questionSetId (질문세트 단위)
const makeKey = (interviewId: number | string, questionSetId?: number | string): string => {
  if (questionSetId !== undefined) {
    return `${interviewId}-${questionSetId}`
  }
  return String(interviewId)
}

export const saveVideoBlob = async (
  interviewId: number | string,
  blob: Blob,
  questionSetId?: number | string,
): Promise<void> => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).put(blob, makeKey(interviewId, questionSetId))
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export const loadVideoBlob = async (
  interviewId: number | string,
  questionSetId?: number | string,
): Promise<Blob | null> => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const request = tx.objectStore(STORE_NAME).get(makeKey(interviewId, questionSetId))
    request.onsuccess = () => resolve(request.result ?? null)
    request.onerror = () => reject(request.error)
  })
}

export const deleteVideoBlob = async (
  interviewId: number | string,
  questionSetId?: number | string,
): Promise<void> => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).delete(makeKey(interviewId, questionSetId))
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}
