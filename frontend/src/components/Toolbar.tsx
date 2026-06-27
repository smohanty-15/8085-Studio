/**
 * Toolbar Component
 *
 * Contains the main action buttons: Assemble, Run, Step, Reset
 * Also shows a status indicator (halted / running / idle)
 */

import React from 'react'

interface ToolbarProps {
  onAssemble: () => void
  onRun: () => void
  onStep: () => void
  onReset: () => void
  onClear: () => void
  isLoading: boolean
  isHalted: boolean
  isAssembled: boolean
}

export function Toolbar({
  onAssemble, onRun, onStep, onReset, onClear,
  isLoading, isHalted, isAssembled
}: ToolbarProps) {

  return (
    <div
      className="flex items-center gap-2 px-4 py-2 border-b"
      style={{
        background: 'var(--bg-secondary)',
        borderColor: 'var(--border-color)',
      }}
    >
      {/* Logo */}
      <div className="flex items-center gap-2 mr-4">
        <div
          className="w-2 h-2 rounded-full animate-pulse"
          style={{ background: 'var(--accent-green)' }}
        />

        <span
          className="font-semibold text-sm tracking-widest"
          style={{ color: 'var(--accent-green)' }}
        >
          8085 STUDIO
        </span>
      </div>

      <div
        className="w-px h-5"
        style={{ background: 'var(--border-color)' }}
      />

      {/* Assemble button - primary action */}
      <Button
        onClick={onAssemble}
        disabled={isLoading}
        color="green"
        title="Assemble (Ctrl+Enter)"
      >
        ⚙ Assemble
      </Button>

      {/* Run button */}
      <Button
        onClick={onRun}
        disabled={isLoading || !isAssembled || isHalted}
        color="blue"
        title="Run until HLT"
      >
        ▶ Run
      </Button>

      {/* Step button */}
      <Button
        onClick={onStep}
        disabled={isLoading || !isAssembled || isHalted}
        color="yellow"
        title="Execute one instruction"
      >
        ⏭ Step
      </Button>

      <div
        className="w-px h-5"
        style={{ background: 'var(--border-color)' }}
      />

      {/* Reset button */}
      <Button
        onClick={onReset}
        disabled={isLoading}
        color="red"
        title="Reset CPU & Memory"
      >
        ↺ Reset
      </Button>

      {/* Clear editor button */}
      <Button
        onClick={onClear}
        disabled={isLoading}
        color="gray"
        title="Clear editor"
      >
        ✕ Clear
      </Button>

      {/* Status indicator */}
      <div className="ml-auto flex items-center gap-2">
        {isLoading && (
          <span
            className="text-xs"
            style={{ color: 'var(--text-secondary)' }}
          >
            Processing...
          </span>
        )}

        {isHalted && !isLoading && (
          <span
            className="text-xs font-medium"
            style={{ color: 'var(--accent-red)' }}
          >
            ● HALTED
          </span>
        )}

        {isAssembled && !isHalted && !isLoading && (
          <span
            className="text-xs font-medium"
            style={{ color: 'var(--accent-green)' }}
          >
            ● READY
          </span>
        )}

        {!isAssembled && !isLoading && (
          <span
            className="text-xs"
            style={{ color: 'var(--text-secondary)' }}
          >
            Not assembled
          </span>
        )}
      </div>
    </div>
  )
}

// Small reusable button inside toolbar
interface ButtonProps {
  onClick: () => void
  disabled?: boolean
  color: 'green' | 'blue' | 'yellow' | 'red' | 'gray'
  title?: string
  children: React.ReactNode
}

function Button({ onClick, disabled, color, title, children }: ButtonProps) {

  const colorMap = {
    green: 'var(--accent-green)',
    blue: 'var(--accent-blue)',
    yellow: 'var(--accent-yellow)',
    red: 'var(--accent-red)',
    gray: 'var(--text-secondary)',
  }

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={title}
      className="px-3 py-1 rounded text-xs font-medium border transition-all duration-150 disabled:opacity-30 disabled:cursor-not-allowed"
      style={{
        color: 'var(--text-secondary)',
        borderColor: 'transparent',
      }}
      onMouseEnter={e => {
        if (disabled) return

        e.currentTarget.style.color = colorMap[color]
        e.currentTarget.style.borderColor = colorMap[color]
        e.currentTarget.style.background =
          `color-mix(in srgb, ${colorMap[color]} 15%, transparent)`
      }}
      onMouseLeave={e => {
        if (disabled) return

        e.currentTarget.style.color = 'var(--text-secondary)'
        e.currentTarget.style.borderColor = 'transparent'
        e.currentTarget.style.background = 'transparent'
      }}
    >
      {children}
    </button>
  )
}