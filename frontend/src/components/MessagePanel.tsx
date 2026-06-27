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
    <div
      className="border-t px-4 py-2 max-h-28 overflow-y-auto"
      style={{
        background: "var(--panel-bg)",
        borderColor: "var(--border-color)",
      }}
    >
      {errors.map((err, i) => (
        <div
          key={i}
          className="flex items-start gap-2 text-[11px] font-mono py-0.5"
          style={{ color: "var(--error-color)" }}
        >
          <span
            className="mt-0.5 flex-shrink-0"
            style={{ color: "var(--error-color)" }}
          >
            ✗
          </span>
          <span>{err}</span>
        </div>
      ))}

      {messages.map((msg, i) => (
        <div
          key={i}
          className="flex items-start gap-2 text-[11px] font-mono py-0.5"
          style={{ color: "var(--success-color)" }}
        >
          <span
            className="mt-0.5 flex-shrink-0"
            style={{ color: "var(--success-color)" }}
          >
            ✓
          </span>
          <span>{msg}</span>
        </div>
      ))}
    </div>
  )
}