export interface OutlineItem {
  id: string
  label: string
  /** 1-based order index, rendered as 01, 02, ... */
  index: number
  /** Shows a signal-warning dot when true (spec §5.3 P1-6). */
  hasIssue?: boolean
}

export interface StickyOutlineBaseProps {
  items: OutlineItem[]
  activeId: string
  onSelect: (id: string) => void
}
