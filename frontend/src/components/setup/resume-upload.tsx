import type { RefObject } from 'react'
import { formatFileSize } from '@/lib/format-utils'

interface ResumeUploadProps {
  resumeFile: File | null
  dragOver: boolean
  isLoading: boolean
  fileInputRef: RefObject<HTMLInputElement>
  onFileSelect: (file: File) => void
  onFileRemove: () => void
  onDrop: (e: React.DragEvent) => void
  onDragOver: () => void
  onDragLeave: () => void
}

export const ResumeUpload = ({
  resumeFile,
  dragOver,
  isLoading,
  fileInputRef,
  onFileSelect,
  onFileRemove,
  onDrop,
  onDragOver,
  onDragLeave,
}: ResumeUploadProps) => {
  return (
    <div className="mt-2 ml-4 motion-safe:animate-fadeIn">
      {/* 안심 문구 */}
      <div className="flex items-center gap-2 rounded-[12px] bg-green-50 px-3 py-2 mb-2">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-green-600 shrink-0">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
        <span className="text-[11px] font-bold text-green-700">
          이력서는 질문 생성에만 사용되며, 어디에도 저장되지 않습니다
        </span>
      </div>

      {!resumeFile ? (
        <div
          onDragOver={(e) => {
            e.preventDefault()
            onDragOver()
          }}
          onDragLeave={onDragLeave}
          onDrop={onDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`flex cursor-pointer items-center gap-3 rounded-[16px] border-2 border-dashed p-4 transition-all ${
            dragOver
              ? 'border-accent bg-accent/5'
              : 'border-border bg-surface hover:border-accent/50 hover:bg-slate-50'
          }`}
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-text-tertiary shrink-0">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          <div>
            <p className="text-xs font-bold text-text-primary">
              PDF 파일을 드래그하거나 클릭하여 업로드
            </p>
            <p className="text-[11px] font-medium text-text-tertiary">
              선택사항 · 최대 10MB
            </p>
          </div>
        </div>
      ) : (
        <div className="flex items-center justify-between rounded-[16px] bg-surface p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/10">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-accent">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
            </div>
            <div>
              <p className="text-xs font-bold text-text-primary">{resumeFile.name}</p>
              <p className="text-[11px] font-medium text-text-tertiary">
                {formatFileSize(resumeFile.size)}
              </p>
            </div>
          </div>
          <button
            onClick={onFileRemove}
            disabled={isLoading}
            className="flex h-7 w-7 items-center justify-center rounded-full bg-white text-text-tertiary transition-all hover:bg-red-50 hover:text-red-500"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      )}
      <input
        ref={fileInputRef}
        type="file"
        accept="application/pdf"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) onFileSelect(file)
        }}
      />
    </div>
  )
}
