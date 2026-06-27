/**
 * ErrorPanel / MessagePanel
 *
 * Shows assembly errors and status messages below the editor.
 * Errors are shown in red, success messages in green.
 */

interface MessagePanelProps {
  errors: string[]
  messages: string[]
}

export function MessagePanel({ errors, messages }: MessagePanelProps) {
  if (errors.length === 0 && messages.length === 0) return null

  return (
    <div className="border-t border-[#30363D] bg-[#0D1117] px-4 py-2 max-h-28 overflow-y-auto">
      {errors.map((err, i) => (
        <div key={i} className="flex items-start gap-2 text-[11px] text-[#FF6B6B] font-mono py-0.5">
          <span className="text-[#FF6B6B] mt-0.5 flex-shrink-0">✗</span>
          <span>{err}</span>
        </div>
      ))}
      {messages.map((msg, i) => (
        <div key={i} className="flex items-start gap-2 text-[11px] text-[#39D353] font-mono py-0.5">
          <span className="text-[#39D353] mt-0.5 flex-shrink-0">✓</span>
          <span>{msg}</span>
        </div>
      ))}
    </div>
  )
}
