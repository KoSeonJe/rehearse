import { useCallback, useEffect, useRef, useState } from 'react'

const MAX_RETRIES = 3
const BASE_DELAY_MS = 1000

interface UseS3UploadReturn {
  upload: (blob: Blob, presignedUrl: string) => Promise<void>
  isUploading: boolean
  progress: number
  error: Error | null
}

export const useS3Upload = (): UseS3UploadReturn => {
  const [isUploading, setIsUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState<Error | null>(null)
  const activeXhrRef = useRef<XMLHttpRequest | null>(null)

  // beforeunload 경고
  useEffect(() => {
    if (!isUploading) return

    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault()
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isUploading])

  const uploadOnce = useCallback((blob: Blob, url: string): Promise<void> => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      activeXhrRef.current = xhr

      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          setProgress(Math.round((e.loaded / e.total) * 100))
        }
      }

      xhr.onload = () => {
        activeXhrRef.current = null
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error(`Upload failed: ${xhr.status} ${xhr.statusText}`))
        }
      }

      xhr.onerror = () => {
        activeXhrRef.current = null
        reject(new Error('Network error during upload'))
      }

      xhr.open('PUT', url)
      xhr.setRequestHeader('Content-Type', blob.type || 'video/webm')
      xhr.send(blob)
    })
  }, [])

  const upload = useCallback(async (blob: Blob, presignedUrl: string) => {
    setIsUploading(true)
    setProgress(0)
    setError(null)

    for (let attempt = 0; attempt < MAX_RETRIES; attempt++) {
      try {
        await uploadOnce(blob, presignedUrl)
        setProgress(100)
        setIsUploading(false)
        return
      } catch (err) {
        if (attempt === MAX_RETRIES - 1) {
          const uploadError = err instanceof Error ? err : new Error('Upload failed')
          setError(uploadError)
          setIsUploading(false)
          throw uploadError
        }
        // 지수 백오프
        const delay = BASE_DELAY_MS * Math.pow(2, attempt)
        await new Promise((r) => setTimeout(r, delay))
        setProgress(0)
      }
    }
  }, [uploadOnce])

  return { upload, isUploading, progress, error }
}
